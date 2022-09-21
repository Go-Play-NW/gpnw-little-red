package org.littlered.dataservices.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/*
 * The default behavior for GET requests with dots in a parameter is to try to smartly guess it's a file name.
 * We don't want this.
 * https://stackoverflow.com/questions/3526523/spring-mvc-pathvariable-getting-truncated
 */

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter
{

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer)
	{
		configurer.favorPathExtension(false);
	}

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer)
	{
		super.configurePathMatch(configurer);
		configurer.setUseSuffixPatternMatch(false);
	}
}