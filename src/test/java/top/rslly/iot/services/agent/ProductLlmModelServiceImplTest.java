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
package top.rslly.iot.services.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.dao.LlmProviderInformationRepository;
import top.rslly.iot.dao.ProductLlmModelRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.UserProductBindRepository;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.dao.WxProductBindRepository;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.LlmProviderInformationEntity;
import top.rslly.iot.models.ProductLlmModelEntity;
import top.rslly.iot.models.UserEntity;
import top.rslly.iot.models.UserProductBindEntity;
import top.rslly.iot.models.WxProductBindEntity;
import top.rslly.iot.models.WxUserEntity;
import top.rslly.iot.param.response.ProductLlmModelResponse;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;

@ExtendWith(MockitoExtension.class)
class ProductLlmModelServiceImplTest {
  @Mock
  private ProductLlmModelRepository productLlmModelRepository;
  @Mock
  private WxProductBindRepository wxProductBindRepository;
  @Mock
  private UserProductBindRepository userProductBindRepository;
  @Mock
  private WxUserRepository wxUserRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private LlmProviderInformationRepository llmProviderInformationRepository;
  @Mock
  private ProductRepository productRepository;
  @InjectMocks
  private ProductLlmModelServiceImpl productLlmModelService;

  @BeforeEach
  void initializeJwtKey() {
    JwtTokenUtil jwtTokenUtil = new JwtTokenUtil();
    ReflectionTestUtils.setField(jwtTokenUtil, "secretKey",
        "myDefaultSecretKeyForDevOnlyChangeInProduction");
    jwtTokenUtil.init();
  }

  @Test
  void pagesAllModelsForAdmin() {
    ProductLlmModelEntity model = model(3, 7, 5);
    when(productLlmModelRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(model), PageRequest.of(1, 10), 21));
    when(llmProviderInformationRepository.findAllByIdIn(List.of(5)))
        .thenReturn(List.of(provider(5, "dashscope-ronger", "ruan")));

    JsonResult<?> response = productLlmModelService.getProductLlmModelPage(
        token("admin", "[ROLE_admin]"), 2, 10, null);

    assertEquals(ResultCode.SUCCESS.getCode(), response.getErrorCode());
    Page<?> page = assertInstanceOf(Page.class, response.getData());
    ProductLlmModelResponse result =
        assertInstanceOf(ProductLlmModelResponse.class, page.getContent().getFirst());
    assertEquals(3, result.getId());
    assertEquals(7, result.getProductId());
    assertEquals(5, result.getProviderId());
    assertEquals("dashscope-ronger (ruan)", result.getProviderName());
    assertEquals(21, page.getTotalElements());
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(productLlmModelRepository).findAll(pageableCaptor.capture());
    Pageable pageable = pageableCaptor.getValue();
    assertEquals(1, pageable.getPageNumber());
    assertEquals(10, pageable.getPageSize());
    assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("id").getDirection());
  }

  @Test
  void pagesOnlyModelsFromUserBoundProducts() {
    UserEntity user = new UserEntity();
    user.setId(12);
    UserProductBindEntity firstBind = bind(7);
    UserProductBindEntity duplicateBind = bind(7);
    UserProductBindEntity secondBind = bind(9);
    ProductLlmModelEntity model = model(4, 9, 6);
    when(userRepository.findAllByUsername("user")).thenReturn(List.of(user));
    when(userProductBindRepository.findAllByUserId(12))
        .thenReturn(List.of(firstBind, duplicateBind, secondBind));
    when(productLlmModelRepository.findAllByProductIdIn(eq(List.of(7, 9)), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(model)));
    when(llmProviderInformationRepository.findAllByIdIn(List.of(6)))
        .thenReturn(List.of(provider(6, "uniapi", null)));

    JsonResult<?> response = productLlmModelService.getProductLlmModelPage(
        token("user", "[ROLE_user]"), 1, 5, null);

    assertEquals(ResultCode.SUCCESS.getCode(), response.getErrorCode());
    Page<?> page = assertInstanceOf(Page.class, response.getData());
    ProductLlmModelResponse result =
        assertInstanceOf(ProductLlmModelResponse.class, page.getContent().getFirst());
    assertEquals(4, result.getId());
    assertEquals("uniapi", result.getProviderName());
  }

  @Test
  void pagesFilteredModelsFromWxUserBoundProduct() {
    WxUserEntity wxUser = new WxUserEntity();
    wxUser.setAppid("app-1");
    wxUser.setOpenid("open-1");
    WxProductBindEntity firstBind = wxBind(7);
    WxProductBindEntity secondBind = wxBind(9);
    ProductLlmModelEntity model = model(5, 9, 6);
    when(wxUserRepository.findAllByName("wx-user")).thenReturn(List.of(wxUser));
    when(wxProductBindRepository.findAllByAppidAndOpenid("app-1", "open-1"))
        .thenReturn(List.of(firstBind, secondBind));
    when(productLlmModelRepository.findAllByProductIdIn(eq(List.of(9)), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(model)));
    when(llmProviderInformationRepository.findAllByIdIn(List.of(6)))
        .thenReturn(List.of(provider(6, "uniapi", "ruan")));

    JsonResult<?> response = productLlmModelService.getProductLlmModelPage(
        token("wx-user", "ROLE_wx_user"), 1, 5, 9);

    assertEquals(ResultCode.SUCCESS.getCode(), response.getErrorCode());
    Page<?> page = assertInstanceOf(Page.class, response.getData());
    ProductLlmModelResponse result =
        assertInstanceOf(ProductLlmModelResponse.class, page.getContent().getFirst());
    assertEquals(9, result.getProductId());
    assertEquals("uniapi (ruan)", result.getProviderName());
  }

  @Test
  void preservesTotalAndSkipsProviderQueryForEmptyPage() {
    Pageable repositoryPageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "id"));
    when(productLlmModelRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(), repositoryPageable, 12));

    JsonResult<?> response = productLlmModelService.getProductLlmModelPage(
        token("admin", "[ROLE_admin]"), 3, 5, null);

    assertEquals(ResultCode.SUCCESS.getCode(), response.getErrorCode());
    Page<?> page = assertInstanceOf(Page.class, response.getData());
    assertEquals(12, page.getTotalElements());
    assertEquals(2, page.getNumber());
    verify(llmProviderInformationRepository, never()).findAllByIdIn(any());
  }

  @Test
  void returnsEmptyPageWhenProductFilterIsOutsideUserScope() {
    UserEntity user = new UserEntity();
    user.setId(12);
    when(userRepository.findAllByUsername("user")).thenReturn(List.of(user));
    when(userProductBindRepository.findAllByUserId(12)).thenReturn(List.of(bind(7)));

    JsonResult<?> response = productLlmModelService.getProductLlmModelPage(
        token("user", "[ROLE_user]"), 1, 5, 9);

    assertEquals(ResultCode.SUCCESS.getCode(), response.getErrorCode());
    Page<?> page = assertInstanceOf(Page.class, response.getData());
    assertEquals(0, page.getTotalElements());
    verify(productLlmModelRepository, never())
        .findAllByProductIdIn(any(), any(Pageable.class));
  }

  @Test
  void rejectsInvalidPaginationBeforeQuerying() {
    JsonResult<?> response = productLlmModelService.getProductLlmModelPage("", 0, 5, null);

    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(), response.getErrorCode());
    verify(productLlmModelRepository, never()).findAll(any(Pageable.class));
  }

  private String token(String username, String role) {
    return JwtTokenUtil.TOKEN_PREFIX + JwtTokenUtil.createToken(username, role);
  }

  private UserProductBindEntity bind(int productId) {
    UserProductBindEntity bind = new UserProductBindEntity();
    bind.setProductId(productId);
    return bind;
  }

  private WxProductBindEntity wxBind(int productId) {
    WxProductBindEntity bind = new WxProductBindEntity();
    bind.setProductId(productId);
    return bind;
  }

  private ProductLlmModelEntity model(int id, int productId, int providerId) {
    ProductLlmModelEntity model = new ProductLlmModelEntity();
    model.setId(id);
    model.setProductId(productId);
    model.setProviderId(providerId);
    return model;
  }

  private LlmProviderInformationEntity provider(int id, String providerName, String userName) {
    LlmProviderInformationEntity provider = new LlmProviderInformationEntity();
    provider.setId(id);
    provider.setProviderName(providerName);
    provider.setUserName(userName);
    return provider;
  }
}
