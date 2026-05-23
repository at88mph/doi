{{/*
Expand the name of the chart.
*/}}
{{- define "doi.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "doi.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "doi.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
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
Selector labels
*/}}
{{- define "doi.selectorLabels" -}}
app.kubernetes.io/name: {{ include "doi.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "doi.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "doi.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the DOI admin certificate secret to mount into /config.
*/}}
{{- define "doi.doiAdminSecretName" -}}
{{- required "app.certificates.doiAdminSecret is required" .Values.app.certificates.doiAdminSecret }}
{{- end }}

{{/*
Create the name of the shared servops certificate secret to mount into /config.
*/}}
{{- define "doi.servopsSecretName" -}}
{{- required "app.certificates.servopsSecret is required" .Values.app.certificates.servopsSecret }}
{{- end }}

{{/*
Validate that the DataCite password Secret is configured.
*/}}
{{- define "doi.validateDatacitePasswordSecret" -}}
{{- $name := .Values.app.datacite.passwordSecret.name -}}
{{- $key := .Values.app.datacite.passwordSecret.key -}}
{{- if not (and $name $key) -}}
{{- fail "app.datacite.passwordSecret.name and app.datacite.passwordSecret.key are required" -}}
{{- end -}}
{{- end }}
