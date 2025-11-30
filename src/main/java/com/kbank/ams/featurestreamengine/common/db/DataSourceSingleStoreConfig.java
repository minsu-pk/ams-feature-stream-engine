package com.kbank.ams.featurestreamengine.common.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.kbank.ams.featurestreamengine.adapter.out.persistence.jpa.repository.singlestore",
        entityManagerFactoryRef = "singlestoreEntityManager",
        transactionManagerRef = "singlestoreTransactionManager"
)
public class DataSourceSingleStoreConfig {
    private final String hibernateDialect;

    public DataSourceSingleStoreConfig(
            @Value("${spring.jpa.properties.hibernate.dialect}") String hibernateDialect) {
        this.hibernateDialect = hibernateDialect;
    }

    @Primary
    @Bean(name = "singlestoreHikariConfig")
    @ConfigurationProperties(prefix = "spring.datasource.hikari.singlestore")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }
    @Primary
    @Bean(name = "singlestoreDataSource")
    public DataSource dataSource() {return new HikariDataSource(hikariConfig());
    }
    @Primary
    @Bean(name = "singlestoreJdbcTemplate")
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }
    @Primary
    @Bean(name = "singlestoreNamedParameterJdbcTemplate")
    @DependsOn("singlestoreDataSource")
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(
            @Qualifier("singlestoreDataSource") DataSource dataSource
    ){
        return new NamedParameterJdbcTemplate(dataSource);
    }
    @Primary
    @Bean(name = "singlestoreEntityManager")
    public LocalContainerEntityManagerFactoryBean entityManager(){
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan(new String[] {"com.kbank.ams.featurestreamengine.adapter.out.persistence.jpa.entity.singlestore"});

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", hibernateDialect);
        em.setJpaPropertyMap(properties);

        return em;
    }
    @Primary
    @Bean(name = "singlestoreTransactionManager")
    public PlatformTransactionManager transactionManager(){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManager().getObject());
        return transactionManager;
    }
}
