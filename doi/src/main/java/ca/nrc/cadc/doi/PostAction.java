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

import ca.nrc.cadc.ac.Group;
import ca.nrc.cadc.ac.GroupURI;
import ca.nrc.cadc.ac.User;
import ca.nrc.cadc.ac.client.GMSClient;
import ca.nrc.cadc.auth.ACIdentityManager;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.DataNode;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeProperty;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.VOSURI;
import java.net.URI;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.jdom2.Element;

/**
 *
 */
public class PostAction extends DOIAction {
    private static final Logger log = Logger.getLogger(PostAction.class);

    private VOSURI target;
    private List<NodeProperty> properties;
    private String AUTHOR_PARAM = "author";
    private String TITLE_PARAM = "title";

    public PostAction() {
        super();
    }

    @Override
    public void doActionImpl() throws Exception {

        Subject subject = AuthenticationUtil.getCurrentSubject();

        // DOINum is parsed out in DOIAction.initRequest()
        if (DOINumInputStr == null) {
            requestType = CREATE_REQUEST;

            // Determine next DOI number
            Node baseNode = vosClient.getNode(astroDataURI.getPath());
            String nextDoiSuffix = generateNextDOINumber((ContainerNode)baseNode);
            log.info("Next DOI suffix is: " + nextDoiSuffix);

            properties = new ArrayList<NodeProperty>();
            NodeProperty isPublic = new NodeProperty(VOS.PROPERTY_URI_ISPUBLIC, "true");

            // Get numeric id for setting doiRequestor property
            ACIdentityManager acIdentMgr = new ACIdentityManager();
            Integer userNumericID = (Integer)acIdentMgr.toOwner(callingSubject);
            NodeProperty doiRequestor = new NodeProperty("doiRequestor", userNumericID.toString());

            // All folders will be publicly readable at first
            // writable only
            properties.add(isPublic);
            properties.add(doiRequestor);

            // Update DOI xml with DOI number
            Element identifier = doiDocRoot.getChild("identifier", doiNamespace);
            identifier.setText(CADC_DOI_PREFIX + "/" + nextDoiSuffix);

            // Create containing folder
            String folderName = DOI_BASE_VOSPACE + "/" + nextDoiSuffix;
            target = new VOSURI(new URI(folderName));
            Node newFolder = new ContainerNode(target, properties);
            vosClient.createNode(newFolder);

            // Create VOSpace data node to house XML doc using doi filename
            String nextDoiFilename = getDOIFilename(nextDoiSuffix);
            log.debug("next doi filename: " + nextDoiFilename);

            String doiFilename = folderName + "/" + nextDoiFilename;
            target = new VOSURI(new URI(doiFilename));
            Node doiFileDataNode = new DataNode(target);
            vosClient.createNode(doiFileDataNode);

            postDoiDocToVospace(doiFilename);

            // Create 'data' folder under containing folder.
            // This is where the calling user will upload their DOI data

            // Create group first in order to apply permissions
            String gmsUriBase = "ivo://cadc.nrc.ca/gms";
            GMSClient gmsClient = new GMSClient(new URI(gmsUriBase));

            String doiGroupName = "DOI-" + nextDoiSuffix;
            String doiGroupURI = gmsUriBase + "?" + doiGroupName;
            GroupURI guri = new GroupURI(new URI(doiGroupURI));

            Group doiRWGroup = new Group(guri);
            User member = new User();
            for (Principal p : callingSubject.getPrincipals()) {
                member.getIdentities().add(p);
            }
            doiRWGroup.getUserMembers().add(member);
            doiRWGroup.getUserAdmins().add(member);
            gmsClient.createGroup(doiRWGroup);

            log.info("group uri being made for " + nextDoiSuffix + ": " + doiGroupURI);

            NodeProperty rGroup = new NodeProperty(VOS.PROPERTY_URI_GROUPREAD,doiGroupURI);
            NodeProperty wGroup = new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE,doiGroupURI);

            properties.add(rGroup);
            properties.add(wGroup);
            String dataFolderName = folderName + "/data";
            target = new VOSURI(new URI(dataFolderName));
            Node newDataFolder = new ContainerNode(target, properties);
            vosClient.createNode(newDataFolder);

            // output document to syncOutput
            writeDoiDocToSyncOutput();
        }
        else {
            throw new UnsupportedOperationException("Editing DOI Metadata not supported.");
            // validate DOI supplied
            // determine if user has access to this DOI
            // validate metadata supplied

        }


    }


    /*
     * Confirm that required information is provided
     * TODO: as been replaced by DoiInlineContentHandler class to parse input streams
     * Q: will key-value pairs be accepted ever?
     */
    private void validateDOIMetadata() {
        // replace 'syncInput' with DOIMetadata object that reader/writer will create
        String author = syncInput.getParameter(AUTHOR_PARAM);

        if (!StringUtil.hasText(author)) {
            throw new IllegalArgumentException("Author is required");
        }
        if (!author.matches("[A-Za-z0-9\\-]+")) {
            throw new IllegalArgumentException("Author can only contain alpha-numeric chars and '-'");
        }

        String title = syncInput.getParameter(TITLE_PARAM);

        if (!StringUtil.hasText(title)) {
            throw new IllegalArgumentException("Title is required");
        }

    }


    /*
     * Generate next DOI, format: YY.####
     */
    private String getNextDOISuffix() {
        // Check VOSpace folder names under AstroDaaCititationDOI, get the 'largest' of the current year
        // 'YY' is a 2 digit year
        DateFormat df = new SimpleDateFormat("yy"); // Just the year, with 2 digits
        String formattedDate = df.format(Calendar.getInstance().getTime());
        return formattedDate + ".####";
    }

    private String generateNextDOINumber(ContainerNode baseNode) {
        // child nodes of baseNode should have name structure YY.XXXX
        // go through list of child nodes
        // extract XXXX
        // track largest
        // add 1
        // reconstruct YY.XXXX structure and return

        // Look into the node list for folders from current year only
        DateFormat df = new SimpleDateFormat("yy"); // Just the year, with 2 digits
        String currentYear = df.format(Calendar.getInstance().getTime());

        Integer maxDoi = 0;
        if (baseNode.getNodes().size() > 0) {
            for( Node childNode : baseNode.getNodes()) {
                String[] nameParts = childNode.getName().split("\\.");
                if (nameParts[0].equals(currentYear)) {
                    int curDoiNum = Integer.parseInt(nameParts[1]);
                    if (curDoiNum > maxDoi) {
                        maxDoi = curDoiNum;
                    }
                }
            }
        }

        maxDoi++;
        String formattedDOI = String.format("%04d", maxDoi);
        return currentYear + "." + formattedDOI;
    }

}
