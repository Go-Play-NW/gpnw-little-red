package org.littlered.dataservices.security.configuration;

import org.littlered.dataservices.security.ApiKeyFilter;
import org.littlered.dataservices.security.JWTAuthenticationFilter;
import org.littlered.dataservices.security.JWTLoginFilter;
import org.littlered.dataservices.security.JWTUtil;
import org.littlered.dataservices.security.password.PhpPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.sql.DataSource;

/**
 * Created by Jeremy on 3/26/2017.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private DataSource dataSource;

	//@Autowired
	//private CustomJwtAuthenticationFilter customJwtAuthenticationFilter;

	@Autowired
	private ApiKeyFilter apiKeyFilter;

	@Autowired
	private JWTAuthenticationFilter jwtAuthenticationFilter;

	@Autowired
	private JWTUtil jwtUtil;

	@Value("${tls.active}")
	private String tlsActive;

	@Value("${db.table_prefix}")
	private String tablePrefix;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable().authorizeRequests()
				// opening access for swagger api documentation
				.antMatchers("/webjars/**").permitAll()
				.antMatchers("/swagger-resources/**").permitAll()
				.antMatchers("/v2/**").permitAll()
				.antMatchers("/swagger-ui.html").permitAll()
				// end swagger configurations
				.antMatchers("/").permitAll()
				.antMatchers(HttpMethod.POST, "/login").permitAll()
				.antMatchers(HttpMethod.POST, "/password/request").permitAll()
				.antMatchers(HttpMethod.POST, "/password/reset").permitAll()
				//.antMatchers(HttpMethod.POST, "/refreshtoken").permitAll()
				.antMatchers(HttpMethod.GET, "/events/all/public").permitAll()
				.antMatchers(HttpMethod.GET, "/events/page/public/**").permitAll()
				.antMatchers(HttpMethod.GET, "/events/spaces/public").permitAll()
				.antMatchers(HttpMethod.GET, "/events/**/spaces/public").permitAll()
//				.antMatchers(HttpMethod.GET, "/events/eventmenus").permitAll()
				.antMatchers(HttpMethod.PUT, "/users/create").permitAll()
				.anyRequest().authenticated()
				.and()
				// We filter the api/login requests
				.addFilterBefore(new JWTLoginFilter("/login", authenticationManager(), jwtUtil),
						UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtAuthenticationFilter,
						JWTLoginFilter.class)
				.addFilterBefore(apiKeyFilter,
						JWTAuthenticationFilter.class)
		;

		if (tlsActive.equals("false")) {
			System.out.println("Disabling TLS");
			http.headers().httpStrictTransportSecurity().disable();
		}

	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {

		auth.jdbcAuthentication().dataSource(dataSource)
				.passwordEncoder(passwordEncoder())
				.usersByUsernameQuery(
						"select user_login, user_pass, true from " + tablePrefix + "users where ? in (user_login, user_email)")
				.authoritiesByUsernameQuery(
						"SELECT " + tablePrefix + "users.user_login, " + tablePrefix + "usermeta.meta_value \n" +
								"  FROM " + tablePrefix + "users \n" +
								"  JOIN " + tablePrefix + "usermeta ON " + tablePrefix + "users.ID = " + tablePrefix + "usermeta.user_id \n" +
								" WHERE " + tablePrefix + "usermeta.meta_key = '" + tablePrefix + "capabilities'\n" +
								"   AND ? in (" + tablePrefix + "users.user_login, " + tablePrefix + "users.user_email)");

	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new PhpPasswordEncoder();
	}

}
