# Core
`portal-core` module provides API for the the users to construct a portal application and SPI for bridge modules to run a portal application on their environment.

Requires Java 6.

## Installing
### Updating pom.xml
Add the following dependency:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-core</artifactId>
    <version>${portal.version}</version>
</dependency>
```

### Loading resources
The following static resources are located in `META-INF/resources/portal/` in the jar. Serve them properly according to your environment. The [documentation](http://www.webjars.org/documentation) of WebJars will give you some help for this.

* [`portal.js`](https://github.com/flowersinthesand/portal-java/blob/master/core/src/main/resources/META-INF/resources/portal/portal.js), [`portal.min.js`](https://github.com/flowersinthesand/portal-java/blob/master/core/src/main/resources/META-INF/resources/portal/portal.min.js): The latest portal.js that Portal for Java supports. If there is a newest version of portal.js than provided one, you can use it unless otherwise noted.

#### WebJars
All the resources of Portal and Portal for Java are available in [WebJars](http://www.webjars.org/). Visit and search `portal` from the list.

## Application
A Portal application is a small bean container which contains beans defined by the user and beans used by the application. Here, the user's todo is to define handlers as a component of presentation layer.

For a declarative programming, a lot of annotations are provided and defined in the package `com.github.flowersinthesand.portal`.

## Annotation

### Bean
Indicates that the annotated class is a bean. The bean is an application component and is instantiated once per each application like singleton. Therefore, all declared fields on the bean must be thread-safe. This annotation is required for all beans.

* `String value() default ""`

The bean name of the class. If a bean name is not provided, the bean name will be the decapitalized form of the class's name.

### Wire
Marks the annotated field as to be wired. The field does not need to be public.

* `String value() default ""`

The bean name to be wired. If the bean name is specified and there is no matching bean with the name and the field's type, wiring will fail. If the bean name is not specified, the annotated field name will be the bean name instead. In this case, even though wiring by the name and the type fails, application will try to find a bean using the type once more.

**Exception**
* if the field type is `App`, the current `App` will be wired. 
* if the field type is `Room`, a room will be found and wired regarding the bean name as the room name.    

### Prepare
Specifies that the annotated method should be be executed after dependency injection is done to perform any initialization. Only public methods with no arguments can be executed.

### On
Defines an annotated method as the event handler. The method should be `public` and a return type of the method doesn't matter.

* `String value() default ""`

The event name. The annotated method's name will be the event name if it's empty.

**Paramter binding**
According to the method signature, the following parameters will be provided.

* `Socket`: The socket instance that sent the event.

### Order
Indicates an execution order of event handlers. Lower values have higher priority.

* `int value()`

The order value.

### Data
Specifies that the event data will be converted to the annotated parameter's type and the expression and set to the annotated parameter. By default, [Jackson](http://wiki.fasterxml.com/JacksonHome) library is used to create an instance from a JSON string. Any object the client sent can be converted into the `Map<String, Object>` type.

* `String value() default ""`

The expression for data. By default, regarding expression as property name, the property of the root data becomes the data to be passed to the handler.

### Reply
Specifies that the annotated parameter or method is a reply callback, and the annotation must be present on one place in all the possible place in a event handler. 

In the method case, after execution a reply callback will be executed regarding the execution result as the callback data.

In the parameter case, the parameter's type should be `Reply.Callback` and the method's return type does not matter. Use this way when you need to execute the callback asynchronously out of the current thread.

#### Callback
Reply callback interface.

* `void done()`
* `void done(Object data)`
* `void fail(Throwable error)`

### Throw
Indicates what exceptions should be handled for the fail callback in the browser side. Without this annotation, the fail callback will never be invoked. Also, this is only valid when `@Reply` is annotated to the method. Data for fail callback is in the form of map contains the handled exception's fully qualified class name (`type`) and message (`message`).  

* `Class<? extends Throwable>[] value() default {}`

Throwable classes to be handled. If no class is specified, the annotated method's exception types will be used. So, either value attribute or exception types have to be specified with this annotation. Unhandled exceptions will be thrown to the container.

## Model 

### App

The App is the standalone application context.

* `static App find()`
* `static App find(String name)`

Finds an application which corresponds to the given name or the first application if the name is not provided from the default repository.

* `App(Options options, Modules... modules)`

Creates a new application with modules. 

* `String name()`

The application name.

* `Object get(String key)`

Returns a value bound with the specified key.

* `App set(String key, Object value)`

Binds a value to this application using the given key.

* `Room room(String name)`

Finds corresponding room or opens new one if it doesn't exist. Utilize this method when trying to access rooms which are not available by injection, namely, are supposed to be created in runtime.

* `Room hall()`

Returns the hall, a specialized room whose name is `hall` and contains every socket that connected to the application.

* `Socket socket(String id)`

Finds a socket by id or returns null if it doesn't exist. Use this method when you need detailed operation in terms of connectivity not entity.

* `Object bean(String name)`
* `<T> T bean(Class<T> class)`
* `<T> T bean(String name, Class<T> class)`

Returns the corresponding bean by name or type from the bean container. Throws IllegalArgumentException if there is no corresponding bean.

* `App register()`

Registers the application to the default repository. Then, the app can be retrieved by `App.find(String name)`. However, injection by container like the Spring or the Guice is more preferred than calling this method and static methods.

* `void close()`

Closes the app, releasing all resources. 

### Options

Options for the core module.

* `String url()`
* `Options url(String url)`

The mapping url. This is required.

* `String name()`
* `Options name(String name)`

The application name. If it is null, the url will be returned instead.

* `Set<String> packages()`
* `Options packageOf(String... packages)`
* `Options packageOf(Class<?>... classes)`
* `Options packageOf(Object... objects)`

Package names that given classes and objects belong to which will be scanned for beans. Actually required.

### Room

The Room is where a group of opened sockets are staying under a specific concept in the real world. Always use a room instead of a socket when handling an entity. For example, a single user can correspond to a single room, but it cannot correspond to a single socket, because multiple sockets from other browsers or devices may indicate the same user simultaneously.

* `String name()`

The room's name.

* `Object get(String key)`

Returns a value bound with the specified key.

* `Room set(String key, Object value)`

Binds a value to this room using the given key.

* `Room add(Socket... sockets)`
* `Room add(Room room)`

Adds the given sockets or sockets in the given room. Only opened socket can be added, and duplicate addition is not possible on account of the Set semantics.

* `Room in(Socket... sockets)`

Creates a new one-time room containing the original room's sockets and the given sockets. Modifications on this room doesn't affect to the original one. This is useful when setting target sockets to receive an event.

* `Room remove(Socket... sockets)`
* `Room remove(Room room)`

Removes the given sockets or sockets in the given room. If one of added sockets is closed, it will be removed from the room as well automatically. Use this method to exclude opened socket.

* `Room out(Socket... sockets)`

Creates a new one-time room containing the original room's sockets except the given sockets. Modifications on this room doesn't affect to the original one. This is useful when setting target sockets to receive an event.

* `Room send(String event)`
* `Room send(String event, Object data)`

Sends an event with data to all sockets in the room.

* `Set<Socket> sockets()`

Returns a cloned set of the sockets contained in the room. Modification on the returned set is not allowed.

* `int size()`

The number of sockets.

* `Room close()`

Closes all the connections of the sockets, clears all sockets and attributes.

### Hall

The Hall is just a specialized room whose name is `hall` and contains every socket that connected to the application.

### Socket

The Socket is a physical bidirectional connectivity between the client and the server and also where the event occurs.

* `boolean opened()`

Whether the socket is opened or closed.

* `String param(String key)`

Getter for the `params` option in the client.

* `Socket send(String event)`
* `Socket send(String event, Object data)`
* `Socket send(String event, Object data, Reply.Callback reply)`

Sends an event with data and attaches a reply callback that takes data which will be sent by the client.

* `void close()`

Closes a connection.