Необходимо написать клиент-серверное сетевое хранилище.

Основные возможности:
- Отправка файлов на сервер, скачивание файлов, просмотр файлов на сервере для каждого пользователя, удаление файлов
  локально/на сервере, переименование файлов локально/на сервере
- Файлы хранятся на сервере в папках с именами пользователей
  (файлы пользователя user1 будут храниться в директории /server_storage/user1/)
- Аутентификация. Для хранения пользователей используем sqlite
- На клиенте должен быть простой графический интерфейс на JavaFX/Swing
- Стек Java EE не используем, для этого есть свои курсы
- Самая основная задача - передача файлов от клиента к серверу, и обратно.
  (вам нужно уметь передавать большие файлы и при этом не "съедать" все ресурсы системы)
- Есть два подхода для передачи файлов: байтовым протоколом, сериализацией
- Кроме файлов надо передавать команды (запрос на скачивание файла, запрос на список файлов, запрос на
  удаление/переименование файлов на сервере, запрос на аутентификацию)

Делать только после реализации основной части:
- Регистрация пользователей
- Древовидную структуру внутри папки пользователя на сервере
- Контрольную сумму передачи файлов
- Синхронизацию папки на клиенте и сервере
- Sharing файлов между клиентами

Самый простой вариант:
Сервер - Netty
Клиент - Netty + java.io