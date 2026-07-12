FROM php:8.1.16-fpm

RUN docker-php-ext-install pdo pdo_mysql

COPY src/ /var/www/html/

EXPOSE 9000
