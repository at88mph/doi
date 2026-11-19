{{/*
Expand the name of the chart.
*/}}
{{- define "doi.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "doi.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "doi.selectorLabels" -}}
app.kubernetes.io/name: {{ include "doi.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "doi.labels" -}}
helm.sh/chart: {{ include "doi.chart" . }}
{{ include "doi.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Use the Gateway API hostname when HTTPRoute is enabled; otherwise use the
first configured Ingress hostname. DOI needs this to construct external URLs.
*/}}
{{- define "doi.hostname" -}}
{{- if .Values.httpRoute.enabled -}}
{{- $hostnames := .Values.httpRoute.hostnames | default (list) -}}
{{- if eq (len $hostnames) 0 -}}
{{- fail "httpRoute.hostnames must contain at least one hostname when httpRoute.enabled is true" -}}
{{- end -}}
{{- index $hostnames 0 -}}
{{- else -}}
{{- $hosts := .Values.ingress.hosts | default (list) -}}
{{- if eq (len $hosts) 0 -}}
{{- fail "ingress.hosts must contain at least one hostname when httpRoute is disabled" -}}
{{- end -}}
{{- index (index $hosts 0) "host" -}}
{{- end -}}
{{- end }}

{{/*
Create the name of the service account to use.
*/}}
{{- define "doi.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (printf "%s-service-account" .Release.Name) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
DataCite credential Secret name.
*/}}
{{- define "doi.dataciteAuthSecretName" -}}
{{- required "application.datacite.auth.existingSecret is required" .Values.application.datacite.auth.existingSecret -}}
{{- end -}}

