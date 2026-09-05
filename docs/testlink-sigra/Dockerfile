FROM php:7.4-apache

# Dependencias del sistema necesarias para compilar las extensiones de PHP
# que TestLink requiere (imágenes, LDAP, zip, XML, etc.)
# Debian bullseye ya salió del soporte activo de seguridad; sus paquetes
# se movieron al archivo histórico permanente. Redirigimos apt allá para
# evitar los 404 del mirror activo.
RUN sed -i \
    -e 's|deb.debian.org|archive.debian.org|g' \
    -e '/security/d' \
    -e '/bullseye-updates/d' \
    /etc/apt/sources.list \
    && echo 'Acquire::Check-Valid-Until "false";' > /etc/apt/apt.conf.d/99no-check-valid-until

RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    unzip \
    libpng-dev \
    libjpeg-dev \
    libfreetype6-dev \
    libzip-dev \
    libxml2-dev \
    libldap2-dev \
    libonig-dev \
    && rm -rf /var/lib/apt/lists/*

# Extensiones de PHP requeridas por TestLink 1.9.x
RUN docker-php-ext-configure gd --with-freetype --with-jpeg \
    && docker-php-ext-configure ldap --with-libdir=lib/x86_64-linux-gnu \
    && docker-php-ext-install -j"$(nproc)" \
        mysqli \
        gd \
        zip \
        xml \
        soap \
        ldap \
        mbstring \
        opcache

# Apache: habilitar mod_rewrite (lo usa TestLink para URLs limpias)
RUN a2enmod rewrite

# Clonar el código fuente OFICIAL de TestLink (rama estable 1.9.x)
# --depth 1 trae solo el último commit, no todo el historial (build más rápido)
RUN cd / \
    && rm -rf /var/www/html \
    && git clone --branch testlink_1_9 --depth 1 \
       https://github.com/TestLinkOpenSourceTRMS/testlink-code.git /var/www/html

# Permisos: TestLink necesita escribir en estas carpetas (subida de archivos,
# generación de logs, y compilación de plantillas Smarty)
# Permisos de todo el código clonado (templates_c de Smarty, etc.)
RUN chown -R www-data:www-data /var/www/html \
    && chmod -R 755 /var/www/html

# TestLink espera estas carpetas en /var/testlink/ por configuración por
# defecto (no en /var/www/html) — ver sección SECURITY del README oficial
RUN mkdir -p /var/testlink/logs /var/testlink/upload_area \
    && chown -R www-data:www-data /var/testlink \
    && chmod -R 755 /var/testlink

# Ajustes de php.ini recomendados por la documentación oficial de TestLink
# (ver sección "CRITICAL PHP.INI Settings" del README oficial)
RUN { \
    echo 'max_input_vars = 3000'; \
    echo 'memory_limit = 256M'; \
    echo 'upload_max_filesize = 20M'; \
    echo 'post_max_size = 20M'; \
    } > /usr/local/etc/php/conf.d/testlink-settings.ini

EXPOSE 80