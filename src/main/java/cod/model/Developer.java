package cod.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import cod.model.enums.Seniority;

@JsonPropertyOrder({ "username", "expertiseArea", "seniority", "performanceScore", "hireDate" })
public class Developer extends User {
    private String hireDate;
    private String expertiseArea;
    private Seniority seniority;
    private double performanceScore = 0.0;

    public String getHireDate() { return hireDate; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }

    public String getExpertiseArea() { return expertiseArea; }
    public void setExpertiseArea(String expertiseArea) { this.expertiseArea = expertiseArea; }

    public Seniority getSeniority() { return seniority; }
    public void setSeniority(Seniority seniority) { this.seniority = seniority; }

    public double getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(double performanceScore) { this.performanceScore = performanceScore; }
}