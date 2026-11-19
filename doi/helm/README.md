# doi

Digital Object Identifier service Helm chart.

This chart deploys the CADC DOI Tomcat service. Non-secret configuration is rendered into a ConfigMap under `/config`; DataCite credentials are read from a Kubernetes Secret and merged into `doi.properties` by an init container. An optional `RsaSignaturePub.key` can also be projected from a Secret for browser cookie validation.

Client certificates (`doiadmin.pem`, `cadcproxy.pem`) are not managed by this chart. Mount your certificate Secrets at the container user's `~/.ssl` directory (tomcat `HOME` is `/usr/share/tomcat` in the [cadc-tomcat](https://github.com/opencadc/docker-base/tree/main/cadc-tomcat) image) using `application.extraVolumes` and `application.extraVolumeMounts` in your deployment values.

## Required Secrets

Create a Secret for DataCite credentials:

```shell
kubectl create secret generic doi-datacite \
  --from-literal=username='<username>' \
  --from-literal=password='<password>'
```

Create the optional Secret containing the cookie-signature public key:

```shell
kubectl create secret generic doi-cookie-signature-public-key \
  --from-file=RsaSignaturePub.key=./RsaSignaturePub.key
```

Set:

```yaml
application:
  datacite:
    auth:
      existingSecret: doi-datacite
  cookieSignaturePublicKey:
    existingSecret:
      name: doi-cookie-signature-public-key
      path: RsaSignaturePub.key
  extraVolumeMounts:
    - name: doiadmin-clientcert
      mountPath: /usr/share/tomcat/.ssl/doiadmin.pem
      subPath: doiadmin.pem
      readOnly: true
    - name: servops-clientcert
      mountPath: /usr/share/tomcat/.ssl/cadcproxy.pem
      subPath: cadcproxy.pem
      readOnly: true
  extraVolumes:
    - name: doiadmin-clientcert
      secret:
        secretName: doiadmin-clientcert
    - name: servops-clientcert
      secret:
        secretName: servops-clientcert
```

## Example Values

Start from `examples/values.example.yaml` and replace the example hostnames, registry IDs, VOSpace URI, DataCite account prefix, and Secret names.

## Test the Chart

Render and lint the chart with non-secret placeholder Secret names:

```shell
helm lint doi/helm \
  --set application.datacite.auth.existingSecret=doi-datacite \
  --set application.config.accountPrefix=10.5072

helm template doi doi/helm \
  --namespace doi \
  --set application.datacite.auth.existingSecret=doi-datacite \
  --set application.config.accountPrefix=10.5072
```

Dry-run against a cluster:

```shell
helm upgrade --install doi doi/helm \
  --namespace doi \
  --create-namespace \
  --values doi/helm/examples/values.example.yaml \
  --dry-run
```

Install after replacing the example values and creating the required Secrets:

```shell
helm upgrade --install doi doi/helm \
  --namespace doi \
  --create-namespace \
  --values <your-values.yaml>
```

Check the workload:

```shell
kubectl -n doi get pods
kubectl -n doi logs deploy/doi-tomcat
kubectl -n doi port-forward svc/doi-tomcat-svc 18080:8080
curl http://localhost:18080/doi/availability
```
