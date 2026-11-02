/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2024.                            (c) 2024.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
************************************************************************
*/

package ca.nrc.cadc.doi;

import ca.nrc.cadc.ac.ACIdentityManager;
import ca.nrc.cadc.ac.client.GMSClient;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.cred.client.CredUtil;
import ca.nrc.cadc.doi.datacite.Identifier;
import ca.nrc.cadc.doi.datacite.Resource;
import ca.nrc.cadc.doi.datacite.Title;
import ca.nrc.cadc.doi.io.DoiXmlWriter;
import ca.nrc.cadc.doi.status.DoiStatus;
import ca.nrc.cadc.doi.status.DoiStatusListJsonWriter;
import ca.nrc.cadc.doi.status.DoiStatusListXmlWriter;
import ca.nrc.cadc.doi.status.Status;
import ca.nrc.cadc.net.OutputStreamWrapper;
import ca.nrc.cadc.net.PermissionDeniedException;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.LocalAuthority;
import ca.nrc.cadc.rest.InlineContentHandler;
import ca.nrc.cadc.rest.RestAction;
import ca.nrc.cadc.util.MultiValuedProperties;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.uws.ExecutionPhase;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;

import org.apache.log4j.Logger;
import org.opencadc.gms.GroupURI;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeProperty;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.client.ClientTransfer;
import org.opencadc.vospace.client.async.RecursiveSetNode;
import org.opencadc.vospace.transfer.Direction;
import org.opencadc.vospace.transfer.Protocol;
import org.opencadc.vospace.transfer.Transfer;

public abstract class DoiAction extends RestAction {
    private static final Logger log = Logger.getLogger(DoiAction.class);

    public static final String STATUS_ACTION = "status";
    public static final String MINT_ACTION = "mint";
    public static final String SEARCH_ACTION = "search";

    protected String doiGroupPrefix;
    protected Subject callingSubject;
    protected Long callersNumericId;
    protected String doiSuffix;
    protected String doiAction;
    protected Boolean includePublic = false;

    protected VospaceDoiClient vospaceDoiClient;
    protected MultiValuedProperties config;
    protected URI vaultResourceID;
    protected URI gmsResourceID;
    protected String accountPrefix;
    protected String parentPath;
    protected GroupURI publisherGroupURI;

    public DoiAction() {
    }

    /**
     * Parse input documents
     * For DOI minting, the service will use the DataCite test system to register the DOI
     * and to make the DOI findable. 
     * For DOI deletion, the service could delete the DOI irrespective of its status. 
     * However this has not been implemented.
     */
    @Override
    protected InlineContentHandler getInlineContentHandler() {
        return new DoiInlineContentHandler();
    }
    
    protected void init()
            throws URISyntaxException, UnknownHostException {
        // load doi properties
        this.config = DoiInitAction.getConfig();
        VOSURI parentVOSURI = DoiInitAction.getParentVOSURI(config);
        this.vaultResourceID = parentVOSURI.getServiceURI();
        this.parentPath = parentVOSURI.getPath();
        this.accountPrefix = config.getFirstPropertyValue(DoiInitAction.DATACITE_ACCOUNT_PREFIX_KEY);
        this.publisherGroupURI = DoiInitAction.getPublisherGroupURI(config);
        this.doiGroupPrefix = config.getFirstPropertyValue(DoiInitAction.DOI_GROUP_PREFIX_KEY);
        if (this.doiGroupPrefix == null) {
            this.doiGroupPrefix = "";
        }

        LocalAuthority localAuthority = new LocalAuthority();
        Set<URI> gmsServices = localAuthority.getResourceIDs(Standards.GMS_SEARCH_10);
        if (gmsServices.isEmpty()) {
            throw new IllegalStateException("GMS service not found");
        } else if (gmsServices.size() > 1) {
            throw new IllegalStateException("multiple GMS services found");
        }
        this.gmsResourceID = gmsServices.iterator().next();

        // get calling subject
        callingSubject = AuthenticationUtil.getCurrentSubject();
        logInfo.setSubject(callingSubject);

        parsePath();

        ACIdentityManager acIdentMgr = new ACIdentityManager();
        this.callersNumericId = (Long) acIdentMgr.toOwner(callingSubject);
        this.vospaceDoiClient = new VospaceDoiClient(vaultResourceID, parentPath,
                callingSubject, includePublic, publisherGroupURI, gmsResourceID);
    }

    protected String getDoiFilename(String suffix) {
        return String.format("%s%s.xml",
                config.getFirstPropertyValue(DoiInitAction.METADATA_PREFIX_KEY), suffix);
    }

    protected VOSURI getVOSURI(String path) {
        return new VOSURI(vaultResourceID, String.format("%s/%s", parentPath, path));
    }

    protected GMSClient getGMSClient() {
        return new GMSClient(gmsResourceID);
    }

    protected Subject getAdminSubject() {
        return SSLUtil.createSubject(new File(System.getProperty("user.home") + "/.ssl/doiadmin.pem"));
    }

    protected void authorize() {
        try {
            CredUtil.checkCredentials(callingSubject);
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            throw new RuntimeException("Failed to check credentials: ", e);
        }
        // authorization, for now, is defined as having a set of principals
        if (callingSubject == null || callingSubject.getPrincipals().isEmpty()) {
            throw new PermissionDeniedException("Unauthorized");
        }
    }

    private void parsePath() {
        String path = syncInput.getPath();
        logInfo.setPath(path);

        if (path != null) {
            String[] parts = path.split("/");
            // Parse the request path to see if a DOI suffix has been provided
            // A full DOI number for CANFAR will be: 10.11570/<DOISuffix>
            if (parts.length > 3) {
                throw new IllegalArgumentException("Bad request: " + path);
            }
            if (parts.length > 0) {
                doiSuffix = parts[0];
                if (parts.length > 1) {
                    doiAction = parts[1];
                }
                // For status requests for individual DOIs, there is need to check
                // to see if the DOI is public in order to provide access.
                if (parts.length > 2 && (parts[2].equals("public"))) {
                    includePublic = true;
                }
            }
        }
    }

    protected boolean checkSubjectsMatch(Subject subA, Subject subB) {
        Set<Principal> subAPrincipals = subA.getPrincipals();
        Set<Principal> subBPrincipals = subB.getPrincipals();

        for (Principal subAPrincipal : subAPrincipals) {
            if (subBPrincipals.contains(subAPrincipal)) {
                // Return if one of the principals matches
                return true;
            }
        }
        return false;
    }

    protected boolean isCallingUserDOIAdmin() {
        return callingSubject != null && checkSubjectsMatch(callingSubject, getAdminSubject());
    }

    protected boolean isCallingUserPublisher() {
        if (publisherGroupURI != null) {
            try {
                return Subject.doAs(callingSubject, (PrivilegedExceptionAction<Boolean>) () -> getGMSClient().isMember(publisherGroupURI));
            } catch (PrivilegedActionException e) {
                log.error(e.getMessage());
            }
        }
        return false;
    }

    protected boolean isCallingUserRequester(Node node) {
        try {
            Long requesterUserId = Long.parseLong(node.getProperty(DOI.VOSPACE_DOI_REQUESTER_PROPERTY).getValue());
            return callersNumericId.equals(requesterUserId);
        } catch (NumberFormatException ex) {
            log.error(String.format("Unable to parse requester uid[%s] for doi: %s",
                    node.getProperty(DOI.VOSPACE_DOI_REQUESTER_PROPERTY).getValue(), node.getName()), ex);
            return false;
        }
    }

    protected List<Node> getAccessibleDOIs() throws Exception {
        Set<Status> readStatus = Set.of(Status.REVIEW_READY, Status.IN_REVIEW, Status.APPROVED, Status.REJECTED);

        List<Node> ownedNodes = new ArrayList<>();
        ContainerNode doiRootNode = vospaceDoiClient.getContainerNode("");
        if (doiRootNode != null) {
            for (Node childNode : doiRootNode.getNodes()) {
                NodeProperty requester = childNode.getProperty(DOI.VOSPACE_DOI_REQUESTER_PROPERTY);

                if (requester != null && requester.getValue() != null) {
                    try {
                        if (childNode.isPublic != null && childNode.isPublic) {
                            ownedNodes.add(childNode);
                            continue;
                        }

                        if (callersNumericId == null) {
                            continue;
                        }

                        Long requesterUserId = Long.parseLong(requester.getValue());
                        if (callersNumericId.equals(requesterUserId) || isCallingUserDOIAdmin()) {
                            ownedNodes.add(childNode);
                            continue;
                        }

                        // if the caller is a publisher and the node status is readable for a publisher
                        NodeProperty statusProperty = childNode.getProperty(DOI.VOSPACE_DOI_STATUS_PROPERTY);
                        if (statusProperty != null && statusProperty.getValue() != null) {
                            Status status = Status.toValue(statusProperty.getValue());
                            if (isCallingUserPublisher() && readStatus.contains(status)) {
                                ownedNodes.add(childNode);
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.error(String.format("Unable to parse requester uid[%s] for doi: %s",
                                requester.getValue(), childNode.getName()), e);
                    }
                }
            }
        }
        return ownedNodes;
    }

    protected void getStatusList(List<Node> nodes) throws Exception {
        List<DoiStatus> doiStatusList = new ArrayList<>();
        for (Node node : nodes) {
            log.debug("StatusList node: " + node.getName());
            // Verify this is a container node before continuing
            if (node instanceof ContainerNode) {
                try {
                    ContainerNode doiContainerNode = vospaceDoiClient.getContainerNode(node.getName());
                    DoiStatus doiStatus = getDoiStatus(node.getName(), doiContainerNode, false);
                    doiStatusList.add(doiStatus);
                    log.debug("added doiStatus: " + doiStatus);
                } catch (Exception ex) {
                    // skip
                    log.debug(String.format("skipping %s because %s", node.getName(), ex.getMessage()));
                }
            } else {
                log.warn("Non-container node found in DOI base directory. Skipping... ");
            }
        }
        log.debug("doiStatusList size: " + doiStatusList.size());

        String docFormat = this.syncInput.getHeader("Accept");
        log.debug("'Accept' value in header is " + docFormat);
        if (docFormat != null && docFormat.contains("application/json")) {
            // json document
            syncOutput.setHeader("Content-Type", "application/json");
            DoiStatusListJsonWriter writer = new DoiStatusListJsonWriter();
            writer.write(doiStatusList, syncOutput.getOutputStream());
        } else {
            // xml document
            syncOutput.setHeader("Content-Type", "text/xml");
            DoiStatusListXmlWriter writer = new DoiStatusListXmlWriter();
            writer.write(doiStatusList, syncOutput.getOutputStream());
        }
    }

    protected DoiStatus getDoiStatus(String doiSuffixString, ContainerNode doiContainerNode, boolean authorize)
            throws Exception {
        DoiStatus doiStatus;
        if (!authorize || vospaceDoiClient.hasCallerReadDOIAccess(doiContainerNode, getAdminSubject())) {
            // get status
            String status = doiContainerNode.getPropertyValue(DOI.VOSPACE_DOI_STATUS_PROPERTY);
            if (StringUtil.hasText(status)
                    && !status.equals(Status.ERROR_REGISTERING.getValue())
                    && !status.equals(Status.ERROR_LOCKING_DATA.getValue())) {
                // update status based on the result of the minting service
                status = updateMintingStatus(doiContainerNode, status);
            }

            // get the data directory
            // TODO why do this when the data directory path is known???
            String dataDirectory = String.format("%s/%s/data", parentPath, doiSuffixString);

            // get title and construct DoiStatus instance
            Title title = null;
            try {
                title = new Title(doiContainerNode.getPropertyValue(DOI.VOSPACE_DOI_TITLE_PROPERTY));
                Identifier identifier = new Identifier(accountPrefix + "/" + doiSuffixString, "DOI");
                doiStatus = new DoiStatus(identifier, title, dataDirectory, Status.toValue(status));
            } catch (Exception ex) {
                Identifier id = new Identifier(accountPrefix + "/" + doiSuffixString, "DOI");
                title = new Title("title");
                doiStatus = new DoiStatus(id, title, dataDirectory, Status.toValue(status));
            }

            // set journalRef
            doiStatus.journalRef = doiContainerNode.getPropertyValue(DOI.VOSPACE_DOI_JOURNAL_PROPERTY);

            // set reviewer
            doiStatus.reviewer = doiContainerNode.getPropertyValue(DOI.VOSPACE_DOI_REVIEWER_PROPERTY);
        } else {
            String msg = "Access Denied to " + doiSuffixString + ".";
            throw new PermissionDeniedException(msg);
        }
        return doiStatus;
    }

    protected String updateMintingStatus(final ContainerNode doiContainerNode, final String status)
            throws Exception {
        return (String) Subject.doAs(getAdminSubject(), (PrivilegedExceptionAction<Object>) () -> {
            // update status based on the result of the minting service
            String localStatus = status;
            String jobURLString = doiContainerNode.getPropertyValue(DOI.VOSPACE_DOI_JOB_URL_PROPERTY);
            if (jobURLString != null) {
                URL jobURL = new URL(jobURLString);
                VOSURI vosuri = new VOSURI(vaultResourceID, String.format("%s/%s", parentPath, doiContainerNode.getName()));
                RecursiveSetNode recursiveSetNode = new RecursiveSetNode(jobURL, doiContainerNode);
                recursiveSetNode.setSchemaValidation(false);

                ExecutionPhase phase;
                try {
                    phase = recursiveSetNode.getPhase(20); // seconds
                } catch (RuntimeException ex) {
                    log.error("error getting recursive lock nodes job status for " + doiContainerNode.getName(), ex);
                    phase = ExecutionPhase.ERROR;
                }

                if (phase != null) {
                    switch (phase) {
                        case COMPLETED:
                        case ARCHIVED:
                            // job finished, set corresponding status
                            if (status.equals(Status.LOCKING_DATA.getValue())) {
                                localStatus = Status.LOCKED_DATA.getValue();
                            } else if (status.equals(Status.REGISTERING.getValue())) {
                                localStatus = Status.MINTED.getValue();
                            }
                            // delete jobURL property
                            doiContainerNode.getProperties().remove(new NodeProperty(DOI.VOSPACE_DOI_JOB_URL_PROPERTY));
                            doiContainerNode.getProperty(DOI.VOSPACE_DOI_STATUS_PROPERTY).setValue(localStatus);
                            vospaceDoiClient.getVOSpaceClient().setNode(vosuri, doiContainerNode);
                            break;
                        case ERROR:
                        case ABORTED:
                        case UNKNOWN:
                        case SUSPENDED:
                        case HELD:
                            // assume job resulted in error, set corresponding status
                            if (status.equals(Status.LOCKING_DATA.getValue())) {
                                localStatus = Status.ERROR_LOCKING_DATA.getValue();
                            } else if (status.equals(Status.REGISTERING.getValue())) {
                                localStatus = Status.ERROR_REGISTERING.getValue();
                            }
                            // delete jobURL property
                            doiContainerNode.getProperties().remove(new NodeProperty(DOI.VOSPACE_DOI_JOB_URL_PROPERTY));
                            doiContainerNode.getProperty(DOI.VOSPACE_DOI_STATUS_PROPERTY).setValue(localStatus);
                            vospaceDoiClient.getVOSpaceClient().setNode(vosuri, doiContainerNode);
                            break;
                        case PENDING:
                        case QUEUED:
                        case EXECUTING:
                            // job is in progress, do nothing
                            break;
                        default:
                            // do nothing
                    }
                }
            }
            return localStatus;
        });
    }

    protected GroupURI createGroupURI(String groupName) {
        String group = String.format("%s?%s", gmsResourceID, groupName);
        return new GroupURI(URI.create(group));
    }

    protected boolean isAlternativeConfiguration() {
        return publisherGroupURI != null;
    }

    protected void uploadDOIDocument(Resource resource, VOSURI docVOSUIRI) throws ResourceNotFoundException {
        Transfer transfer = new Transfer(docVOSUIRI.getURI(), Direction.pushToVoSpace);
        Protocol put = new Protocol(VOS.PROTOCOL_HTTPS_PUT);
        transfer.getProtocols().add(put);

        ClientTransfer clientTransfer = vospaceDoiClient.getVOSpaceClient().createTransfer(transfer);
        DoiAction.DoiOutputStream outStream = new DoiAction.DoiOutputStream(resource);
        clientTransfer.setOutputStreamWrapper(outStream);
        clientTransfer.run();

        if (clientTransfer.getThrowable() != null) {
            log.debug(clientTransfer.getThrowable().getMessage());
            String message = clientTransfer.getThrowable().getMessage();
            if (message != null) {
                if (message.contains("NodeNotFound")) {
                    throw new ResourceNotFoundException(message);
                }
                if (message.contains("PermissionDenied")) {
                    throw new PermissionDeniedException(message);
                }
            }
            throw new RuntimeException((clientTransfer.getThrowable().getMessage()));
        }
    }

    protected static class DoiOutputStream implements OutputStreamWrapper {
        private final Resource streamResource;

        public DoiOutputStream(Resource streamRes) {
            this.streamResource = streamRes; }

        public void write(OutputStream out) throws IOException {
            DoiXmlWriter writer = new DoiXmlWriter();
            writer.write(streamResource, out);
        }
    }

}
