/*
 *   Copyright 2017 Huawei Technologies Co., Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.servicecomb.poc.demo.seckill;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.servicecomb.poc.demo.seckill.dto.CouponDto;
import io.servicecomb.poc.demo.seckill.dto.PromotionDto;
import io.servicecomb.poc.demo.seckill.json.JacksonGeneralFormat;
import io.servicecomb.poc.demo.seckill.repositories.spring.SpringCouponRepository;
import io.servicecomb.poc.demo.seckill.repositories.spring.SpringPromotionRepository;
import io.servicecomb.poc.demo.seckill.repositories.spring.SpringSecKillEventRepository;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = IntegrationTestApplication.class)
@WebAppConfiguration
@AutoConfigureMockMvc
@EnableJms
public class SecKillIntegrationTest {

  private final Format format = new JacksonGeneralFormat();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SpringPromotionRepository promotionRepository;

  @Autowired
  private SpringSecKillEventRepository eventRepository;

  @Autowired
  private SpringCouponRepository couponRepository;

  @Before
  public void setUp() throws Exception {
    eventRepository.deleteAll();
    couponRepository.deleteAll();
    promotionRepository.deleteAll();
  }

  @Test
  public void createPromotionAndGrabSuccessfully() throws Exception {
    MvcResult result = mockMvc.perform(post("/admin/promotions/").contentType(APPLICATION_JSON)
        .content(format.serialize(new PromotionDto(5, 0.7f, new Date()))))
        .andExpect(status().isOk()).andReturn();

    Thread.sleep(1000);

    String promotionId = result.getResponse().getContentAsString();

    mockMvc.perform(post("/command/coupons/").contentType(APPLICATION_JSON)
        .content(format.serialize(new CouponDto<>(promotionId, "zyy"))))
        .andExpect(status().isOk()).andExpect(content().string("Request accepted"));

    Thread.sleep(1000);

    mockMvc.perform(get("/query/coupons/{customerId}", "zyy").contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(
            allOf(
                containsString(promotionId),
                containsString("zyy"))));

    Thread.sleep(1000);
  }

  @Test
  public void failsUpdatePromotionWhenPromotionHadStarted() throws Exception {
    MvcResult result = mockMvc.perform(post("/admin/promotions/").contentType(APPLICATION_JSON)
        .content(format.serialize(new PromotionDto(5, 0.7f, new Date()))))
        .andExpect(status().isOk()).andReturn();

    Thread.sleep(1000);

    String promotionId = result.getResponse().getContentAsString();

    mockMvc.perform(put("/admin/promotions/" + promotionId + "/").contentType(APPLICATION_JSON)
        .content(format.serialize(new PromotionDto(5, 0.7f, new Date()))))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(containsString("PromotionEntity had started and changes is rejected")));
  }
}
