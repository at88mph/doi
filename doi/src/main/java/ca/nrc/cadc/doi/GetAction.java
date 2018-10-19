/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2018.                            (c) 2018.
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


import ca.nrc.cadc.doi.datacite.Resource;
import ca.nrc.cadc.doi.datacite.Title;
import ca.nrc.cadc.doi.status.DoiStatus;
import ca.nrc.cadc.doi.status.DoiStatusJsonWriter;
import ca.nrc.cadc.doi.status.DoiStatusListJsonWriter;
import ca.nrc.cadc.doi.status.DoiStatusListXmlWriter;
import ca.nrc.cadc.doi.status.DoiStatusXmlWriter;
import ca.nrc.cadc.doi.status.Status;
import ca.nrc.cadc.cred.client.CredUtil;
import ca.nrc.cadc.doi.datacite.DoiJsonWriter;
import ca.nrc.cadc.doi.datacite.DoiParsingException;
import ca.nrc.cadc.doi.datacite.DoiXmlReader;
import ca.nrc.cadc.doi.datacite.DoiXmlWriter;

import ca.nrc.cadc.net.InputStreamWrapper;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.Direction;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeNotFoundException;
import ca.nrc.cadc.vos.Protocol;
import ca.nrc.cadc.vos.Transfer;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.VOSURI;
import ca.nrc.cadc.vos.client.ClientTransfer;

import ca.nrc.cadc.vos.client.VOSpaceClient;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;


public class GetAction extends DoiAction {

    private static final Logger log = Logger.getLogger(GetAction.class);

    public GetAction() {
        super();
    }

    @Override
    public void doAction() throws Exception {
        super.init(false);
        
        // need the following statement because the VOSClient is not initializing 
        // the credentials properly
        CredUtil.checkCredentials();
        
        if (super.doiSuffix == null) {
            // get the DoiStatus of all DOI instances for the calling user
            getStatusList();
        }
        else if (super.doiAction != null) {
            // perform the action on the DOI
            performDoiAction();
        }
        else
        {
            getDoi();
        }
    }
    
    private Title getTitle(Resource resource) {
        Title title = null;
        List<Title> titles = resource.getTitles();
        for (Title t : titles)
        {
            if (t.titleType == null)
            {
                title = t;
                break;
            }
        }
        
        return title;
    }
    
    private ContainerNode getContainerNode(String path) throws URISyntaxException, NodeNotFoundException
    {
        VOSURI baseDataURI = new VOSURI(new URI(DOI_BASE_VOSPACE));
        VOSpaceClient vosClient = new VOSpaceClient(baseDataURI.getServiceURI());
        String nodePath = baseDataURI.getPath();
        if (StringUtil.hasText(path))
        {
            nodePath = nodePath + "/" + path;
        }

        return (ContainerNode) vosClient.getNode(nodePath);
    }
    
    private Resource getResource(String doiSuffixString) throws Exception
    {
        VOSURI baseDataURI = new VOSURI(new URI(DOI_BASE_VOSPACE));
        VOSpaceClient vosClient = new VOSpaceClient(baseDataURI.getServiceURI());
        
        VOSURI docDataNode = new VOSURI(
            baseDataURI.toString() + "/" + doiSuffixString + "/" + getDoiFilename(doiSuffixString));
        
        return getDoiDocFromVOSpace(vosClient, docDataNode);
        
    }
    
    private boolean isRequesterNode(Node node)
    {
        boolean isRequesterNode = false;
        String requester = node.getPropertyValue(DOI_VOS_REQUESTER_PROP);
        log.info("requester: " + requester);
        if (StringUtil.hasText(requester))
        {
            isRequesterNode = requester.equals(this.callingSubjectNumericID.toString());
        }
        return isRequesterNode;
    }
    
    private DoiStatus getDoiStatus(String doiSuffixString) throws Exception {
        DoiStatus doiStatus = null;
        ContainerNode doiContainerNode = getContainerNode(doiSuffixString);

        if (isRequesterNode(doiContainerNode))
        {
            String status = doiContainerNode.getPropertyValue(DOI_VOS_STATUS_PROP);
            log.info("node: " + doiContainerNode.getName() + ", status: " + status);
            if (StringUtil.hasText(status))
            {
                String journalRef = doiContainerNode.getPropertyValue(DOI_VOS_JOURNAL_PROP);

                Resource resource = getResource(doiSuffixString);
                Title title = getTitle(resource);
                
                // get the data directory
                String dataDirectory = null;
                List<Node> doiContainedNodes = doiContainerNode.getNodes();
                for (Node node : doiContainedNodes)
                {
                    if (node.getName().equals("data"))
                    {
                        dataDirectory = node.getUri().getPath();
                        break;
                    }
                }
                
                // construct the DOI status
                doiStatus = new DoiStatus(resource.getIdentifier(), title,
                        dataDirectory, Status.toValue(status));
                doiStatus.setJournalRef(journalRef);
            }
        }
        else
        {
            String msg = "No access to " + doiSuffixString + " which was created by someone else.";
            throw new AccessControlException(msg);
        }

        return doiStatus;
    }
    
    private List<Node> getNodeList() throws Exception {
        // VOspace is expected to filter the list of DOIs by user in the future.
        // Currently all DOIs are returned.
        List<Node> containedNodes = new ArrayList<Node>();
        ContainerNode doiContainer = getContainerNode("");
        if (doiContainer != null)
        {
            containedNodes = doiContainer.getNodes();
        }
        return containedNodes;
    }
    
    private void getStatusList() throws Exception {
        List<DoiStatus> doiStatusList = new ArrayList<DoiStatus>();
        List<Node> nodes = getNodeList();
        for (Node node : nodes)
        {
            try
            {
                DoiStatus doiStatus = getDoiStatus(node.getName());
                if (doiStatus != null)
                {
                    doiStatusList.add(doiStatus);
                }
            } 
            catch (AccessControlException ex)
            {
                // skip
                log.debug(ex);
            }
        }

        String docFormat = this.syncInput.getHeader("Accept");
        log.debug("'Accept' value in header is " + docFormat);
        if (docFormat != null && docFormat.contains("application/json"))
        {
            // json document
            syncOutput.setHeader("Content-Type", "application/json");
            DoiStatusListJsonWriter writer = new DoiStatusListJsonWriter();
            writer.write(doiStatusList, syncOutput.getOutputStream());
        }
        else
        {
            // xml document
            syncOutput.setHeader("Content-Type", "text/xml");
            DoiStatusListXmlWriter writer = new DoiStatusListXmlWriter();
            writer.write(doiStatusList, syncOutput.getOutputStream());
        }
    }
    
    private void getDoi() throws Exception {
        Resource resource = getResource(doiSuffix);
        String docFormat = this.syncInput.getHeader("Accept");
        log.debug("'Accept' value in header is " + docFormat);
        if (docFormat != null && docFormat.contains("application/json"))
        {
            // json document
            syncOutput.setHeader("Content-Type", "application/json");
            DoiJsonWriter writer = new DoiJsonWriter();
            writer.write(resource, syncOutput.getOutputStream());
        }
        else
        {
            // xml document
            syncOutput.setHeader("Content-Type", "text/xml");
            DoiXmlWriter writer = new DoiXmlWriter();
            writer.write(resource, syncOutput.getOutputStream());
        }
    }
    
    private void performDoiAction() throws Exception {
        if (doiAction.equals("status"))
        {
            DoiStatus doiStatus = getDoiStatus(doiSuffix);

            String docFormat = this.syncInput.getHeader("Accept");
            log.debug("'Accept' value in header is " + docFormat);
            if (docFormat != null && docFormat.contains("application/json"))
            {
                // json document
                syncOutput.setHeader("Content-Type", "application/json");
                DoiStatusJsonWriter writer = new DoiStatusJsonWriter();
                writer.write(doiStatus, syncOutput.getOutputStream());
            }
            else
            {
                // xml document
                syncOutput.setHeader("Content-Type", "text/xml");
                DoiStatusXmlWriter writer = new DoiStatusXmlWriter();
                writer.write(doiStatus, syncOutput.getOutputStream());
            }
        }
        else
        {
            throw new UnsupportedOperationException("DOI action not implemented: " + doiAction);
        }
    }

    private Resource getDoiDocFromVOSpace(VOSpaceClient vosClient, VOSURI dataNode)
        throws Exception {
        
        List<Protocol> protocols = new ArrayList<Protocol>();
        protocols.add(new Protocol(VOS.PROTOCOL_HTTP_GET));
        protocols.add(new Protocol(VOS.PROTOCOL_HTTPS_GET));
        Transfer transfer = new Transfer(dataNode.getURI(), Direction.pullFromVoSpace, protocols);
        ClientTransfer clientTransfer = vosClient.createTransfer(transfer);
        DoiInputStream doiStream = new DoiInputStream();
        clientTransfer.setInputStreamWrapper(doiStream);
        clientTransfer.run();

        if (clientTransfer.getThrowable() != null) {
            log.debug(clientTransfer.getThrowable().getMessage());
            String message = clientTransfer.getThrowable().getMessage();
            if (message.contains("NodeNotFound")) {
                throw new ResourceNotFoundException(message);
            }
            if (message.contains("PermissionDenied")) {
                throw new AccessControlException(message);
            }
            throw new RuntimeException((clientTransfer.getThrowable().getMessage()));
        }

        return doiStream.getResource();
    }

    private class DoiInputStream implements InputStreamWrapper
    {
        private Resource resource;

        public DoiInputStream() { }

        public void read(InputStream in) throws IOException
        {
            try {
                DoiXmlReader reader = new DoiXmlReader(true);
                resource = reader.read(in);
            } catch (DoiParsingException dpe) {
                throw new IOException(dpe);
            }
        }

        public Resource getResource() {
            return resource;
        }
    }
}
