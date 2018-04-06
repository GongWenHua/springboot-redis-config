package com.thinvent.zhjs.service.report.config.redis;

import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.config.RandomValuePropertySource;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnumerableCompositePropertySource;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertySourcesLoader;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.util.*;

/**
 * @create by SNOW 2018.04.06
 * redis 配置读取
 */
public class RedisConfigApplicationListener implements EnvironmentPostProcessor,
        ApplicationListener<ApplicationEvent>, Ordered {

    private static final String DEFAULT_PROPERTIES = "defaultProperties";

    // Note the order is from least to most specific (last one wins)
    private static final String DEFAULT_SEARCH_LOCATIONS = "redis://";

    private static final String DEFAULT_NAMES = "application";

    public static final String BOOT_REDIS_PROFILE = "boot.config.redis.profiles";

    /**
     * The "active profiles" property name.
     */
    public static final String ACTIVE_PROFILES_PROPERTY = "boot.config.redis.profiles.active";

    /**
     * The "includes profiles" property name.
     */
    public static final String INCLUDE_PROFILES_PROPERTY = "boot.config.redis.profiles.include";

    /**
     * The "config name" property name.
     */
    public static final String CONFIG_NAME_PROPERTY = "boot.config.redis.profiles.config.name";

    /**
     * The "redis host" property name.
     */
    public static final String BOOT_REDIS_HOST = "boot.config.redis.host";

    /**
     * The "redis port" property name.
     */
    public static final String BOOT_REDIS_PORT = "boot.config.redis.port";

    /**
     * The "redis password" property name.
     */
    public static final String BOOT_REDIS_PASSWORD = "boot.config.redis.password";

    /**
     * The "redis database num" property name.
     */
    public static final String BOOT_REDIS_DATABASE = "boot.config.redis.database";

    /**
     * The "redis location prefix" property name.
     */
    public static final String BOOT_REDIS_PREFIX = "boot.config.redis.prefix";

    private final DeferredLog log = new DeferredLog();

    /**
     * The default order for the processor.
     */
    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 11;

    /**
     * Name of the application configuration {@link PropertySource}.
     */
    public static final String APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME = "applicationRedisConfigurationProperties";

    private String searchLocations;

    private String names;

    private int order = DEFAULT_ORDER;

    private final ConversionService conversionService = new DefaultConversionService();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            onApplicationEnvironmentPreparedEvent(
                    (ApplicationEnvironmentPreparedEvent) event);
        }
        if (event instanceof ApplicationPreparedEvent) {
            onApplicationPreparedEvent(event);
        }
    }

    private void onApplicationEnvironmentPreparedEvent(

            ApplicationEnvironmentPreparedEvent event) {

        List<EnvironmentPostProcessor> postProcessors = loadPostProcessors();
        postProcessors.add(this);
        AnnotationAwareOrderComparator.sort(postProcessors);
        for (EnvironmentPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcessEnvironment(event.getEnvironment(),
                    event.getSpringApplication());
        }
    }

    List<EnvironmentPostProcessor> loadPostProcessors() {
        return SpringFactoriesLoader.loadFactories(EnvironmentPostProcessor.class,
                getClass().getClassLoader());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        addPropertySources(environment, application.getResourceLoader());
        configureIgnoreBeanInfo(environment);
        bindToSpringApplication(environment, application);
    }

    private void configureIgnoreBeanInfo(ConfigurableEnvironment environment) {
        if (System.getProperty(
                CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME) == null) {
            RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment,
                    "spring.beaninfo.");
            Boolean ignore = resolver.getProperty("ignore", Boolean.class, Boolean.TRUE);
            System.setProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME,
                    ignore.toString());
        }
    }

    private void onApplicationPreparedEvent(ApplicationEvent event) {
        addPostProcessors(((ApplicationPreparedEvent) event).getApplicationContext());
    }

    /**
     * Add config file property sources to the specified environment.
     *
     * @param environment    the environment to add source to
     * @param resourceLoader the resource loader
     * @see #addPostProcessors(ConfigurableApplicationContext)
     */
    protected void addPropertySources(ConfigurableEnvironment environment,
                                      ResourceLoader resourceLoader) {
        RandomValuePropertySource.addToEnvironment(environment);
        new RedisConfigApplicationListener.Loader(environment, resourceLoader).load();
    }

    /**
     * Bind the environment to the {@link SpringApplication}.
     *
     * @param environment the environment to bind
     * @param application the application to bind to
     */
    protected void bindToSpringApplication(ConfigurableEnvironment environment,
                                           SpringApplication application) {
        PropertiesConfigurationFactory<SpringApplication> binder = new PropertiesConfigurationFactory<SpringApplication>(
                application);
        binder.setTargetName("spring.main");
        binder.setConversionService(this.conversionService);
        binder.setPropertySources(environment.getPropertySources());
        try {
            binder.bindPropertiesToTarget();
        } catch (BindException ex) {
            throw new IllegalStateException("Cannot bind to SpringApplication", ex);
        }
    }

    /**
     * Add appropriate post-processors to post-configure the property-sources.
     *
     * @param context the context to configure
     */
    protected void addPostProcessors(ConfigurableApplicationContext context) {
        context.addBeanFactoryPostProcessor(
                new RedisConfigApplicationListener.PropertySourceOrderingPostProcessor(context));
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    /**
     * Set the search locations that will be considered as a comma-separated list. Each
     * search location should be a directory path (ending in "/") and it will be prefixed
     * by the file names constructed from {@link #setSearchNames(String) search names} and
     * profiles (if any) plus file extensions supported by the properties loaders.
     * Locations are considered in the order specified, with later items taking precedence
     * (like a map merge).
     *
     * @param locations the search locations
     */
    public void setSearchLocations(String locations) {
        Assert.hasLength(locations, "Locations must not be empty");
        this.searchLocations = locations;
    }

    /**
     * Sets the names of the files that should be loaded (excluding file extension) as a
     * comma-separated list.
     *
     * @param names the names to load
     */
    public void setSearchNames(String names) {
        Assert.hasLength(names, "Names must not be empty");
        this.names = names;
    }

    /**
     * {@link BeanFactoryPostProcessor} to re-order our property sources below any
     * {@code @PropertySource} items added by the {@link ConfigurationClassPostProcessor}.
     */
    private class PropertySourceOrderingPostProcessor
            implements BeanFactoryPostProcessor, Ordered {

        private ConfigurableApplicationContext context;

        PropertySourceOrderingPostProcessor(ConfigurableApplicationContext context) {
            this.context = context;
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
                throws BeansException {
            reorderSources(this.context.getEnvironment());
        }

        private void reorderSources(ConfigurableEnvironment environment) {
            RedisConfigApplicationListener.ConfigurationPropertySources
                    .finishAndRelocate(environment.getPropertySources());
            PropertySource<?> defaultProperties = environment.getPropertySources()
                    .remove(DEFAULT_PROPERTIES);
            if (defaultProperties != null) {
                environment.getPropertySources().addLast(defaultProperties);
            }
        }

    }

    /**
     * Loads candidate property sources and configures the active profiles.
     */
    private class Loader {

        private final ConfigurableEnvironment environment;

        private final ResourceLoader resourceLoader;

        private PropertySourcesLoader propertiesLoader;

        private Queue<Profile> profiles;

        private List<Profile> processedProfiles;

        private boolean activatedProfiles;

        private RedisSettings redisSettings = new RedisSettings();


        Loader(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
            this.environment = environment;
            this.loadRedisSettings();
            this.resourceLoader = resourceLoader == null ? new RedisResourceLoader(redisSettings)
                    : resourceLoader;
        }

        public void loadRedisSettings() {
            this.redisSettings.setHost(this.environment.getProperty(BOOT_REDIS_HOST, "127.0.0.1"));
            this.redisSettings.setPort(Integer.valueOf(this.environment.getProperty(BOOT_REDIS_PORT, "6379")));
            this.redisSettings.setPassword(this.environment.getProperty(BOOT_REDIS_PASSWORD, ""));
            this.redisSettings.setDatabase(Integer.valueOf(this.environment.getProperty(BOOT_REDIS_DATABASE, "0")));
            this.redisSettings.setPrefix(this.environment.getProperty(BOOT_REDIS_PREFIX, ""));
        }

        public void load() {
            this.propertiesLoader = new PropertySourcesLoader();
            this.activatedProfiles = false;
            this.profiles = Collections.asLifoQueue(new LinkedList<Profile>());
            this.processedProfiles = new LinkedList<Profile>();

            // Pre-existing active profiles set via Environment.setActiveProfiles()
            // are additional profiles and config files are allowed to add more if
            // they want to, so don't call addActiveProfiles() here.
            Set<Profile> initialActiveProfiles = initializeActiveProfiles();
            this.profiles.addAll(getUnprocessedActiveProfiles(initialActiveProfiles));
            if (this.profiles.isEmpty()) {
                for (String defaultProfileName : this.environment.getDefaultProfiles()) {
                    RedisConfigApplicationListener.Profile defaultProfile = new RedisConfigApplicationListener.Profile(defaultProfileName, true);
                    if (!this.profiles.contains(defaultProfile)) {
                        this.profiles.add(defaultProfile);
                    }
                }
            }

            // The default profile for these purposes is represented as null. We add it
            // last so that it is first out of the queue (active profiles will then
            // override any settings in the defaults when the list is reversed later).
            this.profiles.add(null);

            while (!this.profiles.isEmpty()) {
                RedisConfigApplicationListener.Profile profile = this.profiles.poll();
                for (String location : getSearchLocations()) {
                    log.info("[启动] [配置] redis://" + location);
                    if (!location.endsWith("/")) {
                        // location is a filename already, so don't search for more
                        // filenames
                        load(location, null, profile);
                    } else {
                        for (String name : getSearchNames()) {
                            load(location, name, profile);
                        }
                    }
                }
                this.processedProfiles.add(profile);
            }

            addConfigurationProperties(this.propertiesLoader.getPropertySources());
        }

        private Set<Profile> initializeActiveProfiles() {
            if (!this.environment.containsProperty(ACTIVE_PROFILES_PROPERTY)
                    && !this.environment.containsProperty(INCLUDE_PROFILES_PROPERTY)) {
                return Collections.emptySet();
            }
            // Any pre-existing active profiles set via property sources (e.g. System
            // properties) take precedence over those added in config files.
            RedisConfigApplicationListener.SpringProfiles springProfiles = bindSpringProfiles(
                    this.environment.getPropertySources());
            Set<Profile> activeProfiles = new LinkedHashSet<Profile>(
                    springProfiles.getActiveProfiles());
            activeProfiles.addAll(springProfiles.getIncludeProfiles());
            maybeActivateProfiles(activeProfiles);
            return activeProfiles;
        }

        /**
         * Return the active profiles that have not been processed yet. If a profile is
         * enabled via both {@link #ACTIVE_PROFILES_PROPERTY} and
         * {@link ConfigurableEnvironment#addActiveProfile(String)} it needs to be
         * filtered so that the {@link #ACTIVE_PROFILES_PROPERTY} value takes precedence.
         * <p>
         * Concretely, if the "cloud" profile is enabled via the environment, it will take
         * less precedence that any profile set via the {@link #ACTIVE_PROFILES_PROPERTY}.
         *
         * @param initialActiveProfiles the profiles that have been enabled via
         *                              {@link #ACTIVE_PROFILES_PROPERTY}
         * @return the unprocessed active profiles from the environment to enable
         */
        private List<Profile> getUnprocessedActiveProfiles(
                Set<Profile> initialActiveProfiles) {
            List<Profile> unprocessedActiveProfiles = new ArrayList<Profile>();
            for (String profileName : this.environment.getActiveProfiles()) {
                RedisConfigApplicationListener.Profile profile = new RedisConfigApplicationListener.Profile(profileName);
                if (!initialActiveProfiles.contains(profile)) {
                    unprocessedActiveProfiles.add(profile);
                }
            }
            // Reverse them so the order is the same as from getProfilesForValue()
            // (last one wins when properties are eventually resolved)
            Collections.reverse(unprocessedActiveProfiles);
            return unprocessedActiveProfiles;
        }

        private void load(String location, String name, RedisConfigApplicationListener.Profile profile) {
            String group = "profile=" + (profile == null ? "" : profile);
            if (!StringUtils.hasText(name)) {
                // Try to load directly from the location
                loadIntoGroup(group, location, profile);
            } else {
                // Search for a file with the given name
                for (String ext : this.propertiesLoader.getAllFileExtensions()) {
                    if (profile != null) {
                        // Try the profile-specific file
                        loadIntoGroup(group, location + name + "-" + profile + "." + ext,
                                null);
                        for (RedisConfigApplicationListener.Profile processedProfile : this.processedProfiles) {
                            if (processedProfile != null) {
                                loadIntoGroup(group, location + name + "-"
                                        + processedProfile + "." + ext, profile);
                            }
                        }
                        // Sometimes people put "spring.profiles: dev" in
                        // application-dev.yml (gh-340). Arguably we should try and error
                        // out on that, but we can be kind and load it anyway.
                        loadIntoGroup(group, location + name + "-" + profile + "." + ext,
                                profile);
                    }
                    // Also try the profile-specific section (if any) of the normal file
                    loadIntoGroup(group, location + name + "." + ext, profile);
                }
            }
        }

        private PropertySource<?> loadIntoGroup(String identifier, String location,
                                                RedisConfigApplicationListener.Profile profile) {
            try {
                return doLoadIntoGroup(identifier, location, profile);
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Failed to load property source from location '" + location + "'",
                        ex);
            }
        }

        private PropertySource<?> doLoadIntoGroup(String identifier, String location,
                                                  RedisConfigApplicationListener.Profile profile) throws IOException {
            Resource resource = this.resourceLoader.getResource(location);
            PropertySource<?> propertySource = null;
            StringBuilder msg = new StringBuilder();
            if (resource != null && resource.exists()) {
                String name = "applicationConfig: [" + location + "]";
                String group = "applicationConfig: [" + identifier + "]";
                log.debug("[配置] \nname: " + name + "\ngroup: " + group);
                propertySource = this.propertiesLoader.load(resource, group, name,
                        (profile == null ? null : profile.getName()));
                if (propertySource != null) {
                    msg.append("Loaded ");
                    handleProfileProperties(propertySource);
                } else {
                    msg.append("Skipped (empty) ");
                }
            } else {
                msg.append("Skipped ");
            }
            msg.append("config file ");
            msg.append(getResourceDescription(location, resource));
            if (profile != null) {
                msg.append(" for profile ").append(profile);
            }
            if (resource == null || !resource.exists()) {
                msg.append(" resource not found");
                log.debug(msg.toString());
            } else {
                log.debug(msg.toString());
            }
            return propertySource;
        }

        private String getResourceDescription(String location, Resource resource) {
            String resourceDescription = "'" + location + "'";
            if (resource != null) {
                try {
                    resourceDescription = String.format("'%s' (%s)",
                            resource.getURI().toASCIIString(), location);
                } catch (IOException ex) {
                    // Use the location as the description
                }
            }
            return resourceDescription;
        }

        private void handleProfileProperties(PropertySource<?> propertySource) {
            RedisConfigApplicationListener.SpringProfiles springProfiles = bindSpringProfiles(propertySource);
            maybeActivateProfiles(springProfiles.getActiveProfiles());
            addProfiles(springProfiles.getIncludeProfiles());
        }

        private RedisConfigApplicationListener.SpringProfiles bindSpringProfiles(PropertySource<?> propertySource) {
            MutablePropertySources propertySources = new MutablePropertySources();
            propertySources.addFirst(propertySource);
            return bindSpringProfiles(propertySources);
        }

        private RedisConfigApplicationListener.SpringProfiles bindSpringProfiles(PropertySources propertySources) {
            RedisConfigApplicationListener.SpringProfiles springProfiles = new RedisConfigApplicationListener.SpringProfiles();
            RelaxedDataBinder dataBinder = new RelaxedDataBinder(springProfiles,
            BOOT_REDIS_PROFILE);
            dataBinder.bind(new PropertySourcesPropertyValues(propertySources, false));
            springProfiles.setActive(resolvePlaceholders(springProfiles.getActive()));
            springProfiles.setInclude(resolvePlaceholders(springProfiles.getInclude()));
            return springProfiles;
        }

        private List<String> resolvePlaceholders(List<String> values) {
            List<String> resolved = new ArrayList<String>();
            for (String value : values) {
                resolved.add(this.environment.resolvePlaceholders(value));
            }
            return resolved;
        }

        private void maybeActivateProfiles(Set<Profile> profiles) {
            if (this.activatedProfiles) {
                if (!profiles.isEmpty()) {
                    log.debug("Profiles already activated, '" + profiles
                            + "' will not be applied");
                }
                return;
            }
            if (!profiles.isEmpty()) {
                addProfiles(profiles);
                log.debug("Activated profiles "
                        + StringUtils.collectionToCommaDelimitedString(profiles));
                this.activatedProfiles = true;
                removeUnprocessedDefaultProfiles();
            }
        }

        private void removeUnprocessedDefaultProfiles() {
            for (Iterator<Profile> iterator = this.profiles.iterator(); iterator
                    .hasNext(); ) {
                if (iterator.next().isDefaultProfile()) {
                    iterator.remove();
                }
            }
        }

        private void addProfiles(Set<Profile> profiles) {
            for (RedisConfigApplicationListener.Profile profile : profiles) {
                this.profiles.add(profile);
                if (!environmentHasActiveProfile(profile.getName())) {
                    // If it's already accepted we assume the order was set
                    // intentionally
                    prependProfile(this.environment, profile);
                }
            }
        }

        private boolean environmentHasActiveProfile(String profile) {
            for (String activeProfile : this.environment.getActiveProfiles()) {
                if (activeProfile.equals(profile)) {
                    return true;
                }
            }
            return false;
        }

        private void prependProfile(ConfigurableEnvironment environment,
                                    RedisConfigApplicationListener.Profile profile) {
            Set<String> profiles = new LinkedHashSet<String>();
            environment.getActiveProfiles(); // ensure they are initialized
            // But this one should go first (last wins in a property key clash)
            profiles.add(profile.getName());
            profiles.addAll(Arrays.asList(environment.getActiveProfiles()));
            environment.setActiveProfiles(profiles.toArray(new String[profiles.size()]));
        }

        private Set<String> getSearchLocations() {
            Set<String> locations = new LinkedHashSet<String>();
            locations.addAll(
                    asResolvedSet(RedisConfigApplicationListener.this.searchLocations,
                            DEFAULT_SEARCH_LOCATIONS));
            return locations;
        }

        private Set<String> getSearchNames() {
            if (this.environment.containsProperty(CONFIG_NAME_PROPERTY)) {
                return asResolvedSet(this.environment.getProperty(CONFIG_NAME_PROPERTY),
                        null);
            }
            return asResolvedSet(RedisConfigApplicationListener.this.names, DEFAULT_NAMES);
        }

        private Set<String> asResolvedSet(String value, String fallback) {
            List<String> list = Arrays.asList(StringUtils.trimArrayElements(
                    StringUtils.commaDelimitedListToStringArray(value != null
                            ? this.environment.resolvePlaceholders(value) : fallback)));
            Collections.reverse(list);
            return new LinkedHashSet<String>(list);
        }

        private void addConfigurationProperties(MutablePropertySources sources) {
            List<PropertySource<?>> reorderedSources = new ArrayList<PropertySource<?>>();
            for (PropertySource<?> item : sources) {
                reorderedSources.add(item);
            }
            addConfigurationProperties(
                    new RedisConfigApplicationListener.ConfigurationPropertySources(reorderedSources));
        }

        private void addConfigurationProperties(
                RedisConfigApplicationListener.ConfigurationPropertySources configurationSources) {
            MutablePropertySources existingSources = this.environment
                    .getPropertySources();
            if (existingSources.contains(DEFAULT_PROPERTIES)) {
                existingSources.addBefore(DEFAULT_PROPERTIES, configurationSources);
            } else {
                existingSources.addLast(configurationSources);
            }
        }

    }

    private static class Profile {

        private final String name;

        private final boolean defaultProfile;

        Profile(String name) {
            this(name, false);
        }

        Profile(String name, boolean defaultProfile) {
            Assert.notNull(name, "Name must not be null");
            this.name = name;
            this.defaultProfile = defaultProfile;
        }

        public String getName() {
            return this.name;
        }

        public boolean isDefaultProfile() {
            return this.defaultProfile;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            return ((RedisConfigApplicationListener.Profile) obj).name.equals(this.name);
        }

    }

    /**
     * Holds the configuration {@link PropertySource}s as they are loaded can relocate
     * them once configuration classes have been processed.
     */
    static class ConfigurationPropertySources
            extends EnumerablePropertySource<Collection<PropertySource<?>>> {

        private final Collection<PropertySource<?>> sources;

        private final String[] names;

        ConfigurationPropertySources(Collection<PropertySource<?>> sources) {
            super(APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME, sources);
            this.sources = sources;
            List<String> names = new ArrayList<String>();
            for (PropertySource<?> source : sources) {
                if (source instanceof EnumerablePropertySource) {
                    names.addAll(Arrays.asList(
                            ((EnumerablePropertySource<?>) source).getPropertyNames()));
                }
            }
            this.names = names.toArray(new String[names.size()]);
        }

        @Override
        public Object getProperty(String name) {
            for (PropertySource<?> propertySource : this.sources) {
                Object value = propertySource.getProperty(name);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        public static void finishAndRelocate(MutablePropertySources propertySources) {
            String name = APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME;
            RedisConfigApplicationListener.ConfigurationPropertySources removed = (RedisConfigApplicationListener.ConfigurationPropertySources) propertySources
                    .get(name);
            if (removed != null) {
                for (PropertySource<?> propertySource : removed.sources) {
                    if (propertySource instanceof EnumerableCompositePropertySource) {
                        EnumerableCompositePropertySource composite = (EnumerableCompositePropertySource) propertySource;
                        for (PropertySource<?> nested : composite.getSource()) {
                            propertySources.addAfter(name, nested);
                            name = nested.getName();
                        }
                    } else {
                        propertySources.addAfter(name, propertySource);
                    }
                }
                propertySources.remove(APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME);
            }
        }

        @Override
        public String[] getPropertyNames() {
            return this.names;
        }

    }

    /**
     * Holder for {@code spring.profiles} properties.
     */
    static final class SpringProfiles {

        private List<String> active = new ArrayList<String>();

        private List<String> include = new ArrayList<String>();

        public List<String> getActive() {
            return this.active;
        }

        public void setActive(List<String> active) {
            this.active = active;
        }

        public List<String> getInclude() {
            return this.include;
        }

        public void setInclude(List<String> include) {
            this.include = include;
        }

        Set<Profile> getActiveProfiles() {
            return asProfileSet(this.active);
        }

        Set<Profile> getIncludeProfiles() {
            return asProfileSet(this.include);
        }

        private Set<Profile> asProfileSet(List<String> profileNames) {
            List<Profile> profiles = new ArrayList<Profile>();
            for (String profileName : profileNames) {
                profiles.add(new RedisConfigApplicationListener.Profile(profileName));
            }
            Collections.reverse(profiles);
            return new LinkedHashSet<Profile>(profiles);
        }

    }

}
