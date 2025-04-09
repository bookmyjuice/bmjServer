package online.bmj.www.DTOs;

public class InitialRegisterationResponse {
  private String id;

  public InitialRegisterationResponse(String id) {
    this.id = id;
  }

  public String getMessage() {
    return id;
  }

  public void setMessage(String message) {
    this.id = message;
  }
}
