package com.kidz.workouted.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AdminWebConfig : WebMvcConfigurer {

    override fun addViewControllers(registry: ViewControllerRegistry) {
        // Forward requests to /admin and subpaths to the React index.html
        registry.addViewController("/admin").setViewName("forward:/admin/index.html")
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html")
        registry.addViewController("/admin/{path:[^\\.]+}").setViewName("forward:/admin/index.html")
        registry.addViewController("/admin/*/{path:[^\\.]+}").setViewName("forward:/admin/index.html")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/admin/**")
            .addResourceLocations("classpath:/static/admin/")
    }
}
