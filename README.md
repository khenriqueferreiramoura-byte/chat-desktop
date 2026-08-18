# 🤖 Chat Desktop com JavaFX e Groq

Aplicação desktop de chat desenvolvida em **Java 21** utilizando **JavaFX** para a interface gráfica e a **API da Groq** para comunicação com um modelo de inteligência artificial.

Este projeto foi desenvolvido como uma primeira versão simples, mantendo toda a lógica dentro da classe `Main.java`.

---

## 🚀 Tecnologias utilizadas

- ☕ Java 21
- 🎨 JavaFX 21.0.6
- 📦 Maven
- 🌐 Java HttpClient
- 🤖 Groq API
- 🧠 Modelo `openai/gpt-oss-20b`
- 🧩 JSON
- 🧪 JUnit 5

---

## 📋 Funcionalidades

Atualmente o projeto possui:

- [x] Interface gráfica com JavaFX
- [x] Campo para digitar mensagens
- [x] Botão para enviar mensagens
- [x] Envio através da tecla `ENTER`
- [x] Comunicação com a API da Groq
- [x] Exibição das mensagens do usuário
- [x] Exibição das respostas da IA
- [x] Tratamento básico de erros
- [x] Execução da requisição em uma Thread separada
- [x] Interface não trava durante a requisição

---

# 📁 Estrutura do projeto

```text
chat-desktop/
│
├── pom.xml
│
├── README.md
│
└── src/
    └── main/
        └── java/
            │
            ├── module-info.java
            │
            └── com/
                └── example/
                    └── chatdesktop/
                        │
                        └── Main.java