Remember to change the following in `WsApplication.java` class to match the project name (if changed):

```java
@EnableMongoRepositories(basePackages = {"com.project"})
```

## API Endpoints

### Targets

- `GET /api/targets` - List targets (paginated)
- `GET /api/targets/{id}` - Get target by ID
- `POST /api/targets` - Create new target
- `PUT /api/targets/{id}` - Update target
- `DELETE /api/targets/{id}` - Delete target

### Users

- `GET /user` - List users (paginated)
- `GET /user/{id}` - Get user by ID
- `POST /user` - Create new user
- `PUT /user/{id}` - Update user
- `DELETE /user/{id}` - Delete user

### Organizations

- `GET /api/organizations` - List organizations
- `GET /api/organizations/{id}` - Get organization by ID
- `POST /api/organizations` - Create new organization
- `PUT /api/organizations/{id}` - Update organization
- `DELETE /api/organizations/{id}` - Delete organization

### Authentication

- `POST /authenticate` - Login
- `POST /register` - Register new user
