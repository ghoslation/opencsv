package com.opencsv.bean.mocks;

import java.util.UUID;

/*
 * Copyright 2007 Kyle Miller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class MockBean {
   private String name;
   private String id;
   private String orderNumber;
   private int num;
   private double doubleNum;
   private UUID uuid;

   public MockBean() {}

   public MockBean(String name, String id, String orderNumber, int num, double doubleNum) {
      this.name = name;
      this.id = id;
      this.orderNumber = orderNumber;
      this.num = num;
      this.doubleNum = doubleNum;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getOrderNumber() {
      return orderNumber;
   }

   public void setOrderNumber(String orderNumber) {
      this.orderNumber = orderNumber;
   }

   public int getNum() {
      return num;
   }

   public void setNum(int num) {
      this.num = num;
   }

   public double getDoubleNum() {
      return doubleNum;
   }

   public void setDoubleNum(double doubleNum) {
      this.doubleNum = doubleNum;
   }

   public UUID getUuid() {
      return uuid;
   }

   public void setUuid(UUID uuid) {
      this.uuid = uuid;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MockBean)) return false;

      MockBean mockBean = (MockBean) o;

      if (getNum() != mockBean.getNum()) return false;
      if (Double.compare(mockBean.getDoubleNum(), getDoubleNum()) != 0) return false;
      if (getName() != null ? !getName().equals(mockBean.getName()) : mockBean.getName() != null) return false;
      if (getId() != null ? !getId().equals(mockBean.getId()) : mockBean.getId() != null) return false;
      if (getUuid() != null ? !getUuid().equals(mockBean.getUuid()) : mockBean.getUuid() != null) return false;
      return !(getOrderNumber() != null ? !getOrderNumber().equals(mockBean.getOrderNumber()) : mockBean.getOrderNumber() != null);

   }

   @Override
   public int hashCode() {
      int result;
      long temp;
      result = getName() != null ? getName().hashCode() : 0;
      result = 31 * result + (getId() != null ? getId().hashCode() : 0);
      result = 31 * result + (getOrderNumber() != null ? getOrderNumber().hashCode() : 0);
      result = 31 * result + getNum();
      result = 31 * result + (getUuid() != null ? getUuid().hashCode() : 0);
      temp = Double.doubleToLongBits(getDoubleNum());
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
   }

   @Override
   public String toString() {
      return "MockBean{" +
              "name='" + name + '\'' +
              ", id='" + id + '\'' +
              ", orderNumber='" + orderNumber + '\'' +
              ", num=" + num +
              ", doubleNum=" + doubleNum +
              ", uuid=" + uuid +
              '}';
   }
}
