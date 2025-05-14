<<<<<<<< HEAD:src/main/java/com/bookmyjuice/payload/response/MessageResponse.java
package com.bookmyjuice.payload.response;
========
package online.bmj.www.DTOs;
>>>>>>>> d97884e9565256ce746f426f71499cf53ac87269:src/main/java/online/bmj/www/DTOs/MessageResponse.java

public class MessageResponse {
  private String message;

  public MessageResponse(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
