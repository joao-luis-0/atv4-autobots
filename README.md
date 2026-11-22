# AutoBots — Atividade 4

API desenvolvida em Java 21 e Spring Boot para gerenciamento de usuários, produtos, serviços e vendas. Implementa autenticação com JWT e controle de acesso por perfil: administrador, gerente, vendedor e cliente.

## Requisitos

- Java 21

O projeto inclui o Maven Wrapper, portanto não é necessário instalar o Maven.

## Como executar

No PowerShell, dentro da pasta do projeto, configure as variáveis e inicie a aplicação:

```powershell
$env:ADMIN_LOGIN = "admin"
$env:ADMIN_PASSWORD = "Admin-estudo123!"
$env:JWT_SECRET = "uma-chave-de-estudo-com-pelo-menos-32-bytes"
.\mvnw.cmd spring-boot:run
```

Os valores acima são exemplos para testes locais. A API estará disponível em `http://localhost:8080`.

Por padrão, o banco H2 funciona em memória: os dados são apagados quando a aplicação é encerrada.

## Como testar

### Testes automatizados

Na pasta do projeto, execute:

```powershell
.\mvnw.cmd clean test
```
