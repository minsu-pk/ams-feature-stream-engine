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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.kbank.ams.featurestreamengine.adapter.out.persistence.jpa.repository.goldilocks",
        entityManagerFactoryRef = "goldilocksEntityManager",
        transactionManagerRef = "goldilocksTransactionManager"
)
public class DataSourceGoldilocksConfig {
    private final String hibernateDialect;

    public DataSourceGoldilocksConfig(
            @Value("${spring.jpa.properties.hibernate.dialect}") String hibernateDialect) {
        this.hibernateDialect = hibernateDialect;
    }

    @Bean(name = "goldilocksHikariConfig")
    @ConfigurationProperties(prefix = "spring.datasource.hikari.goldilocks")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }
    @Bean(name = "goldilocksDataSource")
    public DataSource dataSource() {return new HikariDataSource(hikariConfig());
    }
    @Bean(name = "goldilocksJdbcTemplate")
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }
    @Bean(name = "goldilocksNamedParameterJdbcTemplate")
    @DependsOn("goldilocksDataSource")
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(
            @Qualifier("goldilocksDataSource") DataSource dataSource
    ){
        return new NamedParameterJdbcTemplate(dataSource);
    }
    @Bean(name = "goldilocksEntityManager")
    public LocalContainerEntityManagerFactoryBean entityManager(){
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan(new String[] {"com.kbank.ams.featurestreamengine.adapter.out.persistence.jpa.entity.goldilocks"});

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", hibernateDialect);
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "goldilocksTransactionManager")
    public PlatformTransactionManager transactionManager(){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManager().getObject());
        return transactionManager;
    }
}
