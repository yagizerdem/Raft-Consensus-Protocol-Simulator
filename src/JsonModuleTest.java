
import java.util.ArrayList;

public class JsonModuleTest {


    public static void test() throws Exception {
        JsonModule jsonModule = new JsonModule();

        Company company = createSampleCompany();
        String json = jsonModule.Serialize(company);
        System.out.println(json);
        Company c1 = jsonModule.Deserialize(json, Company.class);
        System.out.println(c1);

    }


    @JsonSerializable
    static public class Company {

        @JsonElement
        public String name;

        @JsonElement
        public int foundedYear;

        @JsonElement
        public CEO ceo;

        @JsonElement
        public Department[] departments;

        @JsonElement
        public ArrayList<String> tags;

        @JsonElement
        public ArrayList<Project> projects;

        @Override
        public String toString() {
            return String.format(
                    "Company{name='%s', foundedYear=%d, ceo=%s, departments=%s, tags=%s, projects=%s}",
                    name, foundedYear, ceo, java.util.Arrays.toString(departments), tags, projects
            );
        }
    }

    @JsonSerializable
    static class CEO {

        @JsonElement
        public String fullName;

        @JsonElement
        public int age;

        @JsonElement
        public Address address;

        @Override
        public String toString() {
            return String.format("CEO{fullName='%s', age=%d, address=%s}", fullName, age, address);
        }
    }

    @JsonSerializable
    static class Address {

        @JsonElement
        public String city;

        @JsonElement
        public String country;

        @JsonElement
        public double latitude;

        @JsonElement
        public double longitude;

        @Override
        public String toString() {
            return String.format("Address{city='%s', country='%s', lat=%.4f, lon=%.4f}",
                    city, country, latitude, longitude);
        }
    }

    @JsonSerializable
    static class Department {

        @JsonElement
        public String name;

        @JsonElement
        public int employeeCount;

        @JsonElement
        public ArrayList<Employee> employees;

        @Override
        public String toString() {
            return String.format("Department{name='%s', employeeCount=%d, employees=%s}",
                    name, employeeCount, employees);
        }
    }

    @JsonSerializable
    static class Employee {

        @JsonElement
        public String firstName;

        @JsonElement
        public String lastName;

        @JsonElement
        public double salary;

        @JsonElement
        public Role[] roles;

        @Override
        public String toString() {
            return String.format("Employee{%s %s, salary=%.2f, roles=%s}",
                    firstName, lastName, salary, java.util.Arrays.toString(roles));
        }
    }

    @JsonSerializable
    static class Role {

        @JsonElement
        public String title;

        @JsonElement
        public boolean isActive;

        @Override
        public String toString() {
            return String.format("Role{title='%s', isActive=%s}", title, isActive);
        }
    }

    @JsonSerializable
    static class Project {

        @JsonElement
        public String name;

        @JsonElement
        public ArrayList<String> technologies;

        @JsonElement
        public Task[] tasks;

        @Override
        public String toString() {
            return String.format("Project{name='%s', technologies=%s, tasks=%s}",
                    name, technologies, java.util.Arrays.toString(tasks));
        }
    }

    @JsonSerializable
    static class Task {

        @JsonElement
        public String description;

        @JsonElement
        public boolean completed;

        @Override
        public String toString() {
            return String.format("Task{description='%s', completed=%s}", description, completed);
        }
    }

    public static Company createSampleCompany() {
        Company company = new Company();
        company.name = "Icon Manager Technologies";
        company.foundedYear = 2024;

        // CEO
        CEO ceo = new CEO();
        ceo.fullName = "Yağız Erdem";
        ceo.age = 22;

        Address address = new Address();
        address.city = "İzmir";
        address.country = "Turkey";
        address.latitude = 38.4192;
        address.longitude = 27.1287;
        ceo.address = address;
        company.ceo = ceo;

        // Departments
        Department software = new Department();
        software.name = "Software";
        software.employeeCount = 3;
        software.employees = new ArrayList<>();

        Employee e1 = new Employee();
        e1.firstName = "ahmetcan";
        e1.lastName = "temel";
        e1.salary = 12000.50;
        Role r1 = new Role();
        r1.title = "Backend Developer";
        r1.isActive = true;
        Role r2 = new Role();
        r2.title = "DevOps";
        r2.isActive = false;
        e1.roles = new Role[]{r1, r2};

        Employee e2 = new Employee();
        e2.firstName = "Zülfikar";
        e2.lastName = "Erhan";
        e2.salary = 9500.75;
        Role r3 = new Role();
        r3.title = "Frontend Developer";
        r3.isActive = true;
        e2.roles = new Role[]{r3};

        software.employees.add(e1);
        software.employees.add(e2);

        Department research = new Department();
        research.name = "Research";
        research.employeeCount = 2;
        research.employees = new ArrayList<>();

        company.departments = new Department[]{software, research};

        // Tags
        company.tags = new ArrayList<>();
        company.tags.add("AI");
        company.tags.add("ML");
        company.tags.add("Distributed Systems");

        // Projects
        company.projects = new ArrayList<>();

        Project p1 = new Project();
        p1.name = "Melodiq";
        p1.technologies = new ArrayList<>();
        p1.technologies.add("C#");
        p1.technologies.add("React");
        p1.technologies.add("SQLite");
        p1.tasks = new Task[]{
                createTask("Build core player engine", true),
                createTask("Implement Deezer API", false)
        };

        Project p2 = new Project();
        p2.name = "Icon Manager";
        p2.technologies = new ArrayList<>();
        p2.technologies.add("Go");
        p2.technologies.add("TypeScript");
        p2.technologies.add(".NET");
        p2.tasks = new Task[]{
                createTask("Design icon caching layer", true)
        };

        company.projects.add(p1);
        company.projects.add(p2);

        return company;
    }

    private static Task createTask(String desc, boolean done) {
        Task t = new Task();
        t.description = desc;
        t.completed = done;
        return t;
    }

}
