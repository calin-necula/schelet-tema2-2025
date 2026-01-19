package cod.model;

import cod.model.enums.Seniority;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "username", "expertiseArea", "seniority", "performanceScore", "hireDate" })
public final class Developer extends User {
    private String hireDate;
    private String expertiseArea;
    private Seniority seniority;
    private double performanceScore = 0.0;

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(final String hireDate) {
        this.hireDate = hireDate;
    }

    public String getExpertiseArea() {
        return expertiseArea;
    }

    public void setExpertiseArea(final String expertiseArea) {
        this.expertiseArea = expertiseArea;
    }

    public Seniority getSeniority() {
        return seniority;
    }

    public void setSeniority(final Seniority seniority) {
        this.seniority = seniority;
    }

    public double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(final double performanceScore) {
        this.performanceScore = performanceScore;
    }
}
