package io.github.giuliapais.api.models;

public class IdPresentErrorMessage {
    private String errorMessage;
    private int statusCode;

    public IdPresentErrorMessage() {
    }

    public IdPresentErrorMessage(String errorMessage, int statusCode) {
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

}
