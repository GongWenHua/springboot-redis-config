# springboot-redis-config

### 1 整体解决思路

    spring boot 有关启动配置流程
        SpringApplication.run()
            DefaultApplicationArguments(args)=>applicationArguments
                SpringApplicationRunListeners(logger, getSpringFactoriesInstances(SpringApplicationRunListener.class, types, this, args))
                    getSpringFactoriesInstances(SpringApplicationRunListener.class, types, this, args)
            getRunListeners(args)=>listeners
            prepareEnvironment(listeners,applicationArguments)
                environmentPrepared(environment)
                    multicastEvent(new ApplicationEnvironmentPreparedEvent(this.application, this.args, environment))	多播ApplicationEnvironmentPreparedEvent
                    
		
		执行ConfigFileApplicationListener	ApplicationEnvironmentPreparedEvent事件进行执行配置文件的加载


    redis配置解决方案
        自定义类似ConfigFileApplicationListener的RedisConfigApplicationListener
        在META-INF/spring.factories
            添加如下配置：# Application Listeners
                            org.springframework.context.ApplicationListener=com.thinvent.zhjs.service.report.config.redis.RedisConfigApplicationListener
                        使RedisConfigApplicationListener生效
            
    
### 2 配置示例
![image](http://github.com/itmyhome2013/readme_add_pic/raw/master/images/nongshalie.jpg)