# HostelNews

HostelNews is a web application designed to manage news posts and updates for hostels. The application allows hostel administrators to create, edit, and manage posts, which can include descriptions, images, and display options.

## Table of Contents
- [Features](#features)
- [Technologies](#technologies)
- [Installation](#installation)
- [Usage](#usage)

## Features
- **User Authentication**: Secure login and user management.
- **Post Management**: Create, edit, and delete posts with a customizable display.
- **File Upload**: Attach images or files to posts.
- **Gallery Link**: Link to external image galleries within posts.

## Technologies
- **Frontend**: HTML, CSS, JavaScript
- **Backend**: Java Spring Boot
- **Database**: MariaDB

## Installation
1. **Clone the repository**
   ```bash
   git clone https://github.com/stypekmaslo6/HostelNews.git
   cd HostelNews

2. **Once you create database in any MariaDB supporting application (e.g. [DBeaver](#https://dbeaver.io/download/)), create tables using this SQL, and provide database connection to app.properties.**
    ```
   -- Table: users
    CREATE TABLE `users` (
      `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_polish_ci NOT NULL,
      `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_polish_ci NOT NULL,
      `id` int(2) NOT NULL DEFAULT 0,
      PRIMARY KEY (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
    
    -- Table: posts
   CREATE TABLE `posts` (
      `ID` bigint(20) NOT NULL AUTO_INCREMENT,
      `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_polish_ci NOT NULL,
      `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_polish_ci DEFAULT NULL,
      `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_polish_ci DEFAULT NULL,
      `show_desc` tinyint(1) DEFAULT NULL,
      `gallery_link` varchar(255) DEFAULT NULL,
      `files_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_polish_ci DEFAULT NULL,
      `thumbnail_url` varchar(255) DEFAULT NULL,
      `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
      `like_count` int(11) DEFAULT 0,
      PRIMARY KEY (`ID`),
      KEY `username_key` (`username`),
      CONSTRAINT `username_key` FOREIGN KEY (`username`) REFERENCES `users` (`username`)
    ) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
    
    -- Table: comments
    CREATE TABLE `comments` (
      `comment_id` bigint(20) NOT NULL AUTO_INCREMENT,
      `post_id` bigint(20) NOT NULL,
      `comment_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_polish_ci DEFAULT NULL,
      `user` varchar(255) DEFAULT NULL,
      `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
      PRIMARY KEY (`comment_id`),
      KEY `FKh4c7lvsc298whoyd4w9ta25cr` (`post_id`),
      CONSTRAINT `FKh4c7lvsc298whoyd4w9ta25cr` FOREIGN KEY (`post_id`) REFERENCES `posts` (`ID`)
    ) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
    
    -- Table: likes
    CREATE TABLE `likes` (
      `like_id` bigint(20) NOT NULL AUTO_INCREMENT,
      `username` varchar(255) DEFAULT NULL,
      `post_id` bigint(20) DEFAULT NULL,
      PRIMARY KEY (`like_id`),
      UNIQUE KEY `username` (`username`,`post_id`),
      KEY `FKry8tnr4x2vwemv2bb0h5hyl0x` (`post_id`),
      CONSTRAINT `FKry8tnr4x2vwemv2bb0h5hyl0x` FOREIGN KEY (`post_id`) REFERENCES `posts` (`ID`)
    ) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=latin2 COLLATE=latin2_general_ci;
    
    -- Table: login_statistics
   CREATE TABLE `login_statistics` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
      `year` int(11) NOT NULL,
      `month` int(11) NOT NULL,
      `login_count` int(11) DEFAULT 0,
      PRIMARY KEY (`id`),
      UNIQUE KEY `year` (`year`,`month`)
    ) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=latin2 COLLATE=latin2_general_ci;
    ```
    
4. **Run the application**
   ```bash
   mvn spring_boot:run
   ```

## Usage
1. **Login**: Access the admin panel by logging in with your credentials. Admin is determined by ID in users table. If one has 1 in column ID, the user has admin permissions.
2. **Create a Post**: Use the "Create Post" form to add a new post with a title, description, gallery link, and image.
3. **Edit a Post**: Open an existing post to modify its contents or settings.
4. **Delete a Post**: Remove posts when they are no longer relevant.
