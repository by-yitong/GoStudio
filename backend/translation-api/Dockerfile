FROM golang:1.23-alpine AS build
WORKDIR /src
COPY go.mod ./
RUN go mod download
COPY *.go ./
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/translation-api .

FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /out/translation-api /translation-api
ENV TRANSLATION_BACKEND_ADDR=:8080
EXPOSE 8080
USER nonroot:nonroot
ENTRYPOINT ["/translation-api"]
