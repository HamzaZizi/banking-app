FROM nginx:1.27-alpine

COPY fraud.json /usr/share/nginx/html/fraud.json
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 9090
