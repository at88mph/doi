<%@ page language="java" contentType="text/html; charset=UTF-8" session="false" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="baseURL" value='<%= System.getenv("CANFAR_WEB_HOST") %>' />

<!-- Default to current host. -->
<c:if test="${empty baseURL}">
  <c:set var="req" value="${pageContext.request}" />
  <c:set var="url">${req.requestURL}</c:set>
  <c:set var="uri" value="${req.requestURI}" />
  <c:set var="baseURL" value="${fn:substring(url, 0, fn:length(url) - fn:length(uri))}" />
</c:if>

<c:set var="resourceCapabilitiesEndPoint" value="${baseURL}/reg/resource-caps" />
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html lang="en">

  <head>
    <meta charset='utf-8'>
    <meta http-equiv="X-UA-Compatible" content="chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <base href="${fn:substring(url, 0, fn:length(url) - fn:length(uri))}${req.contextPath}/" />

    <c:import url="${baseURL}/canfar/includes/_page_top_styles.shtml" />
    <link rel="stylesheet" type="text/css"
          href="<c:out value=" ${baseURL}/citation/css/citation.css " />" media="screen"
    />
    <link rel="stylesheet" type="text/css"
          href="<c:out value=" ${baseURL}/cadcVOTV/css/jquery-ui-1.11.4.min.css " />" media="screen"
    />

    <!-- Located in ROOT.war -->
    <script type="application/javascript" src="${baseURL}/canfar/javascript/jquery-2.2.4.min.js"></script>
    <script type="application/javascript" src="${baseURL}/canfar/javascript/bootstrap.min.js"></script>

    <!--[if lt IE 9]>
        <script src="/html5shiv.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->
    <title>Data Citation</title>
  </head>

  <body>
    <c:import url="${baseURL}/canfar/includes/_application_header.shtml" />
    <div class="container-fluid fill">
      <div class="row fill">
        <div role="main" class="col-sm-12 col-md-12 main fill">
          <div class="inner fill">
            <section id="main_content" class="fill">

              <h2 class="doi-page-header">
                <a id="canfar-doi" class="anchor" href="#canfar-doi" aria-hidden="true">
                  <span aria-hidden="true" class="octicon octicon-link"></span>
                </a>Data Citation
              </h2>

              <div class="doi-authenticated">
                <div id="doi_metadata" class="panel panel-default doi-panel">
                  <div class="panel-heading doi-panel-heading"><h4>DOI Metadata</h4>
                  </div>
                  <div class="progress doi-progress-bar-container">
                    <div class="progress-bar progress-bar-success doi-progress-bar"
                         role="progressbar" aria-valuenow="100" aria-valuemin="100" aria-valuemax="100">
                    </div>
                  </div>
                  <div class="panel-body doi-panel-body">

                    <!-- Noficiation and Alert bars -->
                    <div class="alert alert-danger hidden">
                      <strong id="status_code">444</strong>&nbsp;&nbsp;<span id="error_msg">Server error</span>
                    </div>

                    <div class="alert alert-success hidden">
                      <span id="alert_msg"></span>
                    </div>

                    <div class="doi_not_authenticated hidden"><button type="submit" class="btn btn-primary" id="doi_login_button">
                      <i>Login Required...</i></button>
                    </div>
                    <!-- Form starts -->
                    <div class="doi-form-body hidden">
                      <form id="doi_request_form" class="form-horizontal">
                        <!-- Data DOI Number -->
                        <div class="form-group doi-form-group">
                          <label for="doi_number" class="col-sm-3 control-label" id="doi_number_label">Data DOI Reference</label>
                          <div class="col-sm-3 doi-form">
                            <input type="text" class="form-control doi-form doi-form-input" id="doi_number" name="doiNumber"
                                   disabled="disabled" readonly />
                          </div>
                          <div class="col-sm-3 doi-display doi-number hidden">
                          </div>
                          <div id="doi_minted" class="btn btn-success col-sm-2 doi-form-info hidden "><i>MINTED</i></div>
                        </div>

                        <!-- Publication Title -->
                        <div class="form-group">
                          <label for="doi_title" class="col-sm-3 control-label" id="doi_title_label">Title</label>
                          <div class="col-sm-6">
                            <input type="text" class="form-control doi-form doi-form-input" id="doi_title" name="title"
                                   placeholder="Title" tabindex="1" required/>
                            <div class="doi-display doi-title hidden">
                            </div>
                          </div>
                        </div>

                        <!-- Publication Language -->
                        <div class="form-group hidden">
                          <label for="doi_language" class="col-sm-3 control-label" id="doi_language_label">Language</label>
                          <div id="doi_language" class="col-sm-3">
                            <label class="radio-inline"><input type="radio" name="doiLanguage" value="en" checked>en</label>
                            <label class="radio-inline"><input type="radio" name="doiLanguage" value="fr">fr</label>
                          </div>
                        </div>

                        <div class="form-group">
                          <label for="doi_author" class="col-sm-3 control-label" id="doi_first_name_label">First Author</label>
                            <div class="col-sm-6">
                              <input type="text" class="form-control doi-form doi-form-input" id="doi_author" name="firstAuthor"
                                     placeholder="family name, given name" tabindex="1" required/>
                              <div class="doi-display doi-author hidden">
                              </div>
                            </div>
                            <div class="col-sm-3 doi-form doi-form-info"><i>format: family name, given name</i></div>
                        </div>

                      <div class="form-group">
                          <label for="doi_additional_authors" class="col-sm-3 control-label" id="doi_addtl_authors_label">Additional Authors (Optional)</label>
                          <div class="col-sm-6">
                              <div id="doi_additional_authors"></div>
                              <div>
                              <label for="doi_add_author" class="col-sm-2 control-label doi-form doi-vertical-align"><i>add author</i></label>
                              <button type="button" class="btn btn-default doi-small-button doi-form glyphicon glyphicon-plus" id="doi_add_author" tabindex="5"></button>
                              </div>
                          </div>
                      </div>

                        <!-- Journal Reference - will appear on landing page -->
                        <div class="form-group">
                          <label for="doi_journal_ref" class="col-sm-3 control-label" id="doi_journal_ref_label">Journal Ref</label>
                          <div class="col-sm-6">
                            <input type="text" class="form-control doi-form doi-form-input" id="doi_journal_ref" name="journalRef"
                                   placeholder="Journal  Reference" tabindex="3" required/>
                            <div class="doi-display doi-journal-ref hidden">
                            </div>
                          </div>
                        </div>

                        <!-- Buttons -->
                        <div class="form-group">
                          <div class="col-sm-offset-3 col-sm-10">
                            <div class="button-group" role="group">
                              <button type="submit" class="btn btn-primary doi-form" id="doi_action_button" tabindex="5">Request</button>
                              <button type="reset" class="btn btn-default doi-button" id="doi_form_reset_button" tabindex="6">Reset</button>
                              <button type="mint" class="btn btn-success doi-button doi-form hidden" id="doi_form_mint_button">Mint</button>
                              <button type="delete" class="btn btn-danger doi-button doi-form hidden" id="doi_form_delete_button">Delete</button>
                            </div>
                          </div>
                        </div>
                      </form>
                    </div>
                  </div>
                </div>

                <!-- DOI Metadata panel -->
                <div id="doi_related" class="panel panel-default doi-panel hidden">
                  <div class="panel-heading doi-panel-heading">
                    <h4>Related Information</h4>
                  </div>
                  <div class="panel-body doi-panel-body">
                    <div class="row">
                      <label for="doi_status" class="col-sm-3 control-label text-right " id="doi_status_label">Status</label>
                      <div class="col-sm-9">
                        <span id="doi_status"></span>
                      </div>
                    </div>

                    <div class="row">
                      <label for="doi_data_dir" class="col-sm-3 control-label text-right " id="doi_data_dir_label">Data Directory</label>
                      <div class="col-sm-9">
                        <span id="doi_data_dir">data dir</span>
                      </div>
                    </div>

                    <div class="row">
                      <label for="doi_landing_page" class="col-sm-3 control-label text-right " id="doi_landing_page_label">URL</label>
                      <div class="col-sm-9">
                        <span id="doi_landing_page"><i>not available yet</i></span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Info/Error Modal -->
              <div class="modal fade" id="info_modal" tabindex="-1" role="dialog" aria-labelledby="exampleModalLongTitle" aria-hidden="true">
                <div class="modal-dialog" role="document">
                  <div class="modal-content">
                    <div class="modal-header">
                      <h5 class="modal-title" id="infoModalLongTitle"></h5>
                      <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                      </button>
                    </div>
                    <div class="modal-body">
                      <span class="info-span"></span>
                    </div>
                    <div id="infoThanks" class="modal-footer">
                      <button type="button" class="btn btn-default" data-dismiss="modal">Thanks</button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Content ends -->
            </section>
          </div>
        </div>
      </div>
    </div>

    <script type="text/javascript" src="http://apps.canfar.net/cadcJS/javascript/org.opencadc.js"></script>
    <script type="text/javascript" src="http://apps.canfar.net/cadcJS/javascript/cadc.uri.js"></script>
    <script type="text/javascript" src="http://apps.canfar.net/canfar/javascript/cadc.user.js"></script>
    <script type="text/javascript" src="//apps.canfar.net/cadcJS/javascript/cadc.registry-client.js"></script>
    <script type="text/javascript" src="//www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadc/bootstrap/js/cadc.user.js"></script>
    <script type="text/javascript" src="//www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadc/bootstrap/js/login.js"></script>
    <script type="application/javascript" src="<c:out value=" ${baseURL}/citation/js/citation_page.js" />"></script>
    <script type="application/javascript" src="<c:out value=" ${baseURL}/citation/js/citation_request.js" />"></script>

    <script type="application/javascript">
      $(document).ready(function() {

        // Set up controller for Data Citation Request page
        request_js = new cadc.web.citation.CitationRequest({resourceCapabilitiesEndPoint: '${resourceCapabilitiesEndPoint}'})
        request_js.init()

      });

    </script>

  </body>

</html>

