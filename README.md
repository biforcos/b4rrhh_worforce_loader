# b4rrhh-workforce-loader

CLI externo para crear empleados de forma masiva contra las APIs publicas canonicas de B4RRHH.

## Ejecutar

```bash
mvn spring-boot:run
```

## Dry-run

Por defecto `loader.run.dry-run: true`, por lo que se generan empleados y payloads de hire sin invocar el backend.
Para ejecutar llamadas reales, configurar `loader.run.dry-run: false` en `application.yml`.
