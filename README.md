
# What are Inlay Hints?
They are a feature used to assist the writer of a code by eliminating the type ambiguity from the static code through adding local hints to variables or methods parameters, just like `java` or `C#` hints!

## What is Dart Inlay Hints Project?
The project purpose is to implement one of a kind feature called (Dart Inlay Hints) on IntelliJ idea 2021.2.4 IDE as a (Plugin). It implements the missing inlay hints feature for dart code.

## What Are the Currently Supported Versions?
The plugin currently support version 2021.2.4.
Will update the plugin to the newer versions soon!

## What Are the currently supported hints’ types?
The stable version of the plugin supports the variables hints completely.
The methods are not well supported due to missing API endpoints for methods in dart analyzer. 
I will develop an extension for dart analyzer and custom endpoint for the required functionality then add the hints feature for the methods.

# How To Use Dart Inlay Hints
Simply right click on any dart file and turn on dart inlay hints.
![Raw Code](https://github.com/MohaAmiry/DartHints/assets/65380552/5830d7fb-4bfe-4e0f-9271-a036526c957a)
the generated hints will be displayed right before the variables and methods.
![Hints Code](https://github.com/MohaAmiry/DartHints/assets/65380552/956810be-537f-45b9-889b-24561e8d7a80)


# Used Languages and Frameworks
This project is developed using java, and extends IntelliJ Idea plugins platform. It used dart SDK as a base to visit the required types and detect the static types.

# Motivations Behind Implementation
When I was a university student, I started with c++ and java, one of the features that I really liked was the `inlay hints` in IntelliJ platform! It was very helpful as I was programming with java. When I started learning dart, it was missing this feature, so I decided to implement it as a plugin for IntelliJ Idea, and write a full academic documentation on it as a software.

# What Are the Obstacles of Plugin Development?
When it comes to non-commercial development, the learning sources are very limited, and the official documentation barely touches the surface. The only way to learn plugin development is by reading third party plugins and observe how they work and try to understand the process.

# Future Aspects
The project will continue to develop and implement the same feature for methods’ parameters.

# How Does it Work?
this plugin relies on `dart analyzer` API from dart SDK, and several IntelliJ Idea packages
-	`com.intellij.codeinsight`: This package is used in the platform to present various in-editor information, such as documents, syntax error highlight, coloring, hints, indentations, folding, and more. For this project only hints functionalities were used (com.intellij.codeinsight.hints.*) which is used to add inline text ignored by the compiler and has other multiple properties.
- `com.intellij.openapi`: This package is used in various functionalities in the platform, editor features, abstract IO features, IDE actions (every button in the IDE represents an action), It is major part of the IDE. Only editor and action features were used (com.intellij.openapi.editor.*) which is used to access the editor and manipulate it, (com.intellij.openapi.actionsSystem.*) which is used create a way for user to interact with the plugin behavior -a clickable button for example-.
-	`com.intellij.psi`: This package works as A layer between the language and all platform features. This project uses two different PSI layers, the first one is intellij idea implementation of PSI layer to communicate with all platform features, the second one is Dart language implementation of PSI layer to recognize Dart code and manipulate it. The libraries used are (com.intellij.psi.*) which was used to manipulate intelij idea components as elements, files as this report will discuss later, (com.jetbrains.lang.dart.psi.*).

# Software Analysis UML Diagrams
this section will view the required diagram in order to understand the overall workflow of Dart Inlay Hints.
## Use Case Diagram
![DIH Use Case](https://github.com/MohaAmiry/DartHints/assets/65380552/a22d0232-0e1b-46f1-8bfe-b1a43bf359e8)

## Functional Flow Block Diagram
![Dart Hints FFBD](https://github.com/MohaAmiry/DartHints/assets/65380552/82d67b8f-c420-466e-a847-34222f9d0c34)


# Documentation
this documentation [here](https://github.com/MohaAmiry/DartHints/files/14041840/DartHints.Document.Clear.pdf), is a very detailed academic documentation on the plugin and it explains every details on the working packages, and how are they used.




