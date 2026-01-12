package cod.model;
import java.util.List;

public class Manager extends User {
    private String hireDate;
    private List<String> subordinates;

    public String getHireDate() { return hireDate; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    public List<String> getSubordinates() { return subordinates; }
    public void setSubordinates(List<String> subordinates) { this.subordinates = subordinates; }
}