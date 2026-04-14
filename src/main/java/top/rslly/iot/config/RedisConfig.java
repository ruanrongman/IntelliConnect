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
package top.rslly.iot.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.BooleanSerializer;
import jakarta.annotation.Nullable;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;
import org.springframework.data.redis.core.*;

@Configuration
@EnableCaching // 开启注解
public class RedisConfig extends CachingConfigurerSupport {

  public class ByteArrayRedisSerializer implements RedisSerializer<byte[]> {

    @Override
    public byte[] serialize(@Nullable byte[] bytes) throws SerializationException {
      // 如果输入是 null，返回 null 以匹配 Spring Data Redis 的约定
      return bytes;
    }

    @Override
    public byte[] deserialize(@Nullable byte[] bytes) throws SerializationException {
      // 如果输入是 null，返回 null
      return bytes;
    }
  }

  /**
   * retemplate相关配置
   * 
   * @param factory
   * @return
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    // 配置连接工厂
    template.setConnectionFactory(factory);

    // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值（默认使用JDK的序列化方式）
    Jackson2JsonRedisSerializer jacksonSeial = new Jackson2JsonRedisSerializer(Object.class);

    ObjectMapper om = new ObjectMapper();
    // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
    om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    // 指定序列化输入的类型，类必须是非final修饰的，final修饰的类，比如String,Integer等会跑出异常
    om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    jacksonSeial.setObjectMapper(om);

    // 值采用json序列化
    template.setValueSerializer(jacksonSeial);
    // 使用StringRedisSerializer来序列化和反序列化redis的key值
    template.setKeySerializer(new StringRedisSerializer());

    // 设置hash key 和value序列化模式
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(jacksonSeial);
    template.afterPropertiesSet();

    return template;
  }

  @Bean
  public RedisTemplate<String, byte[]> bytesRedisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, byte[]> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);

    // Key 使用 String 序列化器
    RedisSerializer<String> keySerializer = new StringRedisSerializer();
    template.setKeySerializer(keySerializer);
    template.setHashKeySerializer(keySerializer);

    // Value 使用 ByteArray 序列化器，直接存储字节数组
    RedisSerializer<byte[]> valueSerializer = new ByteArrayRedisSerializer();
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);

    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public RedisTemplate<String, Boolean> boolRedisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Boolean> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);

    // Key 使用 String 序列化器
    RedisSerializer<String> keySerializer = new StringRedisSerializer();
    template.setKeySerializer(keySerializer);
    template.setHashKeySerializer(keySerializer);

    // Value 使用自定义 Boolean 序列化器
    RedisSerializer<Boolean> valueSerializer = new BooleanRedisSerializer();
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);

    template.afterPropertiesSet();
    return template;
  }

  /**
   * 自定义 Boolean 序列化器，将 Boolean 存储为 "1"/"0"
   */
  public static class BooleanRedisSerializer implements RedisSerializer<Boolean> {

    private static final byte[] TRUE_BYTES = "1".getBytes();
    private static final byte[] FALSE_BYTES = "0".getBytes();

    @Override
    public byte[] serialize(@Nullable Boolean bool) throws RuntimeException {
      if (bool == null) {
        return new byte[0];
      }
      return bool ? TRUE_BYTES : FALSE_BYTES;
    }

    @Override
    public Boolean deserialize(@Nullable byte[] bytes) throws RuntimeException {
      if (bytes == null || bytes.length == 0) {
        return null;
      }
      // 支持 "1" / "true" / "yes" / "on" 等，此处简单判断第一个字节是否为 '1'
      return bytes.length > 0 && bytes[0] == '1';
    }
  }

  /**
   * 对hash类型的数据操作
   *
   * @param redisTemplate
   * @return
   */
  @Bean
  public HashOperations<String, String, Object> hashOperations(
      RedisTemplate<String, Object> redisTemplate) {
    return redisTemplate.opsForHash();
  }

  /**
   * 对redis字符串类型数据操作
   *
   * @param redisTemplate
   * @return
   */
  @Bean
  public ValueOperations<String, Object> valueOperations(
      RedisTemplate<String, Object> redisTemplate) {
    return redisTemplate.opsForValue();
  }

  /**
   * 对链表类型的数据操作
   *
   * @param redisTemplate
   * @return
   */
  @Bean
  public ListOperations<String, Object> listOperations(
      RedisTemplate<String, Object> redisTemplate) {
    return redisTemplate.opsForList();
  }

  /**
   * 对无序集合类型的数据操作
   *
   * @param redisTemplate
   * @return
   */
  @Bean
  public SetOperations<String, Object> setOperations(RedisTemplate<String, Object> redisTemplate) {
    return redisTemplate.opsForSet();
  }

  /**
   * 对有序集合类型的数据操作
   *
   * @param redisTemplate
   * @return
   */
  @Bean
  public ZSetOperations<String, Object> zSetOperations(
      RedisTemplate<String, Object> redisTemplate) {
    return redisTemplate.opsForZSet();
  }

}

