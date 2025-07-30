package app.model;

import java.util.List;

/**
 * Модель вакансии с данными, полученными со страницы hh.ru
 */
public class Vacancy {
    private String id;
    private String title;
    private String company;
    private String location;
    private String salaryRaw;
    private List<String> skills;
    private String description;
    private String employmentType;
    private String workFormat; // remote/office/hybrid
    private String sourceUrl;

    // Конструкторы
    public Vacancy() {}

    public Vacancy(String id, String title, String company, String location, 
                   String salaryRaw, List<String> skills, String description, 
                   String employmentType, String workFormat, String sourceUrl) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.salaryRaw = salaryRaw;
        this.skills = skills;
        this.description = description;
        this.employmentType = employmentType;
        this.workFormat = workFormat;
        this.sourceUrl = sourceUrl;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSalaryRaw() { return salaryRaw; }
    public void setSalaryRaw(String salaryRaw) { this.salaryRaw = salaryRaw; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public String getWorkFormat() { return workFormat; }
    public void setWorkFormat(String workFormat) { this.workFormat = workFormat; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    @Override
    public String toString() {
        return "Vacancy{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", company='" + company + '\'' +
                ", location='" + location + '\'' +
                ", salaryRaw='" + salaryRaw + '\'' +
                ", skills=" + skills +
                ", employmentType='" + employmentType + '\'' +
                ", workFormat='" + workFormat + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                '}';
    }
} 