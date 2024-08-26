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
package top.rslly.iot.models;

import org.influxdb.annotation.Measurement;
import top.rslly.iot.utility.influxdb.ano.Tag;
import top.rslly.iot.utility.influxdb.ano.TimeColumn;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Measurement(name = "event_storage")
@Table(name = "event_storage", schema = "cwliot1.8", catalog = "")
public class EventStorageEntity {
  @TimeColumn
  private long time;
  @Tag
  private int deviceId;
  @Tag
  private String jsonKey;
  private String value;
  private int eventId;
  private String characteristic;

  @Id
  @Column(name = "time")
  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  @Basic
  @Column(name = "device_id")
  public int getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(int deviceId) {
    this.deviceId = deviceId;
  }

  @Basic
  @Column(name = "json_key")
  public String getJsonKey() {
    return jsonKey;
  }

  public void setJsonKey(String jsonKey) {
    this.jsonKey = jsonKey;
  }

  @Basic
  @Column(name = "value")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Basic
  @Column(name = "event_id")
  public int getEventId() {
    return eventId;
  }

  public void setEventId(int eventId) {
    this.eventId = eventId;
  }

  @Basic
  @Column(name = "characteristic")
  public String getCharacteristic() {
    return characteristic;
  }

  public void setCharacteristic(String characteristic) {
    this.characteristic = characteristic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    EventStorageEntity that = (EventStorageEntity) o;
    return time == that.time && deviceId == that.deviceId && eventId == that.eventId
        && Objects.equals(jsonKey, that.jsonKey) && Objects.equals(value, that.value)
        && Objects.equals(characteristic, that.characteristic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(time, deviceId, jsonKey, value, eventId, characteristic);
  }
}
