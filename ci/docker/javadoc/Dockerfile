FROM nginx:1.18-alpine

WORKDIR /usr/share/nginx/html/

# Clean the default public folder
RUN rm -fr * .??*

COPY apidocs /usr/share/nginx/html
