# unit test template

Generate unit test code based on the following technology stack and project requirements:
- Technology stack: Java 17, Spring Boot 3.2, MyBatis 3.5, Oracle 19
- Testing framework: JUnit 5, Mockito
- Follow unit test best practice 
- Cover all methods: Ensure tests cover all public methods, including edge cases and exception scenarios.
- Mock dependencies: Use Mockito to mock all external dependencies (such as MyBatis Mapper, Service, etc.).
- Verify logic: Validate the logical correctness of methods, including return values, state changes, and exception throwing.
- OpenAPI specification: Verify API input and output formats according to OpenAPI documentation, ensuring compliance with the specification.
- High coverage: Ensure test coverage reaches above 90%, including branch coverage and path coverage.
- Test class structure: 
Use @ExtendWith(MockitoExtension.class) annotation
Use @Mock annotation to mock dependencies
Use @InjectMocks annotation to inject the object being tested
Include @BeforeEach method to initialize test data
Each test method should use @Test annotation and include a clear description.
- Test scenarios:
Normal scenarios: Verify method behavior under normal input.
Edge cases: Verify method behavior under boundary conditions (such as null values, maximum values, minimum values).
Exception scenarios: Verify method behavior when given exceptional input or when dependencies fail (e.g., throwing specific exceptions).
- Example test method naming: test{MethodName}_When{Condition}_Should{ExpectedBehavior}
For example: testGetUserById_WhenIdExists_ShouldReturnUser

Please generate unit test code based on the above requirements.
