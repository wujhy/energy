package com.shanhe.framework.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import com.shanhe.common.utils.StringUtils;

/**
 * Mybatis支持*匹配扫描包
 *
 * @author wjh
 * @since 2025/6/6
 */
@Slf4j
@Configuration
public class MyBatisConfig {
    @Autowired
    private Environment env;

    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    public static String setTypeAliasesPackage(String typeAliasesPackage)
    {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        List<String> allResult = new ArrayList<>();
        try {
            for (String aliasesPackage : typeAliasesPackage.split(",")) {
                List<String> result = new ArrayList<>();
                aliasesPackage = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                        + ClassUtils.convertClassNameToResourcePath(aliasesPackage.trim()) + "/" + DEFAULT_RESOURCE_PATTERN;
                Resource[] resources = resolver.getResources(aliasesPackage);
                if (resources.length > 0) {
                    MetadataReader metadataReader;
                    for (Resource resource : resources) {
                        if (resource.isReadable()) {
                            metadataReader = metadataReaderFactory.getMetadataReader(resource);
                            try {
                                result.add(Class.forName(metadataReader.getClassMetadata().getClassName()).getPackage().getName());
                            } catch (ClassNotFoundException e) {
                                log.error("ClassNotFoundException{}", e.getMessage());
                            }
                        }
                    }
                }
                if (!result.isEmpty()) {
                    HashSet<String> hashResult = new HashSet<>(result);
                    allResult.addAll(hashResult);
                }
            }
            if (!allResult.isEmpty()) {
                typeAliasesPackage = String.join(",", allResult.toArray(new String[0]));
            } else {
                throw new RuntimeException("mybatis typeAliasesPackage 路径扫描错误,参数typeAliasesPackage:" + typeAliasesPackage + "未找到任何包");
            }
        } catch (IOException e) {
            log.error("IOException{}", e.getMessage());
        }
        return typeAliasesPackage;
    }

    public Resource[] resolveMapperLocations(String[] mapperLocations)
    {
        // 创建一个ResourcePatternResolver对象
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        // 创建一个List对象，用于存储Resource对象
        List<Resource> resources = new ArrayList<>();
        // 如果mapperLocations不为空
        if (mapperLocations != null)
        {
            // 遍历mapperLocations数组
            for (String mapperLocation : mapperLocations)
            {
                try {
                    // 根据mapperLocation获取Resource对象数组
                    Resource[] mappers = resourceResolver.getResources(mapperLocation);
                    // 将Resource对象数组添加到List中
                    resources.addAll(Arrays.asList(mappers));
                } catch (IOException e) {
                    // 如果发生IOException，打印错误信息
                    log.error("IOException{}", e.getMessage());
                }
            }
        }
        // 将List转换为Resource数组并返回
        return resources.toArray(new Resource[0]);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception
    {
        String typeAliasesPackage = env.getProperty("mybatis.typeAliasesPackage");
        String mapperLocations = env.getProperty("mybatis.mapperLocations");
        String configLocation = env.getProperty("mybatis.configLocation");
        if (typeAliasesPackage != null) {
            typeAliasesPackage = setTypeAliasesPackage(typeAliasesPackage);
        }
        VFS.addImplClass(SpringBootVFS.class);

        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setTypeAliasesPackage(typeAliasesPackage);
        sessionFactory.setMapperLocations(resolveMapperLocations(StringUtils.split(mapperLocations, ",")));
        if (configLocation != null) {
            sessionFactory.setConfigLocation(new DefaultResourceLoader().getResource(configLocation));
        }
        return sessionFactory.getObject();
    }
}