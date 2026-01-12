package cod.model;

import cod.model.enums.Seniority;

public class Developer extends User {
    private String hireDate;
    private String expertiseArea;
    private Seniority seniority;

    public String getHireDate() { return hireDate; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    public String getExpertiseArea() { return expertiseArea; }
    public void setExpertiseArea(String expertiseArea) { this.expertiseArea = expertiseArea; }
    public Seniority getSeniority() { return seniority; }
    public void setSeniority(Seniority seniority) { this.seniority = seniority; }
}