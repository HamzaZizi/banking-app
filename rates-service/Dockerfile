FROM nginx:1.27-alpine

COPY rates.json /usr/share/nginx/html/rates.json
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 9090
