/**
 * Copyright © 2023-2030 The ruanrongman Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.rslly.iot.utility.influxdb;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProxyMapperRegister
    implements BeanDefinitionRegistryPostProcessor, ResourceLoaderAware, ApplicationContextAware {
  private static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

  private MetadataReaderFactory metadataReaderFactory;

  private ResourcePatternResolver resourcePatternResolver;

  private ApplicationContext applicationContext;

  private String mapperLocation;

  public ProxyMapperRegister(ResourcePatternResolver resourcePatternResolver,
      ApplicationContext applicationContext, String mapperLocation) {
    this.resourcePatternResolver = resourcePatternResolver;
    this.applicationContext = applicationContext;
    this.mapperLocation = mapperLocation;
  }

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry)
      throws BeansException {
    // 获取启动类所在包
    List<String> packages = new ArrayList<>();
    packages.add(mapperLocation);

    // 开始扫描包，获取字节码
    Set<Class<?>> beanClazzSet = scannerPackages(packages.get(0));
    for (Class beanClazz : beanClazzSet) {
      // 判断是否是需要被代理的接口
      // BeanDefinition构建器
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanClazz);
      GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();

      // 在这里，我们可以给该对象的属性注入对应的实例。
      definition.getConstructorArgumentValues().addGenericArgumentValue(beanClazz);

      // 定义Bean工程(最终会用上面add的构造函数参数值作为参数调用RepositoryFactory的构造方法)
      definition.setBeanClass(InfluxProxyMapperFactory.class);
      // 这里采用的是byType方式注入，类似的还有byName等
      definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
      String simpleName = beanClazz.getSimpleName();
      // 首字母小写注入容器
      simpleName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
      beanDefinitionRegistry.registerBeanDefinition(simpleName, definition);
    }
  }

  @Override
  public void postProcessBeanFactory(
      ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
    this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
  }

  private Boolean needProxy(Class beanClazz) {
    Class[] interfaces = beanClazz.getInterfaces();
    for (Class anInterface : interfaces) {
      if (anInterface == InfluxDBBaseMapper.class) {
        return true;
      }
    }
    return false;
  }

  private Set<Class<?>> scannerPackages(String basePackage) {
    Set<Class<?>> set = new LinkedHashSet<>();
    // 此处固定写法即可,含义就是包及子包下的所有类
    String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
        + resolveBasePackage(basePackage) + '/' + DEFAULT_RESOURCE_PATTERN;
    try {
      Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
      for (Resource resource : resources) {
        if (resource.isReadable()) {
          MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
          String className = metadataReader.getClassMetadata().getClassName();
          Class<?> clazz;
          try {
            clazz = Class.forName(className);
            if (needProxy(clazz)) {
              set.add(clazz);
            }
          } catch (ClassNotFoundException e) {
            e.printStackTrace();
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return set;
  }

  private String resolveBasePackage(String basePackage) {
    // 将类名转换为资源路径
    return ClassUtils.convertClassNameToResourcePath(
        // 解析占位符
        this.applicationContext.getEnvironment().resolveRequiredPlaceholders(basePackage));
  }
}
