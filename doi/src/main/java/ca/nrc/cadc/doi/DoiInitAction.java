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
 *  : 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.doi;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.rest.InitAction;
import ca.nrc.cadc.util.MultiValuedProperties;
import ca.nrc.cadc.util.PropertiesReader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Principal;
import java.util.Objects;
import java.util.Set;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.opencadc.gms.GroupURI;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.client.VOSpaceClient;

public class DoiInitAction extends InitAction {
    private static final Logger log = Logger.getLogger(DoiInitAction.class);

    public static final String DOI_KEY = "ca.nrc.cadc.doi";
    public static final String VOSPACE_PARENT_URI_KEY = DOI_KEY + ".vospaceParentUri";
    public static final String METADATA_PREFIX_KEY = DOI_KEY + ".metaDataPrefix";
    public static final String DOI_GROUP_PREFIX_KEY = DOI_KEY + ".groupPrefix";
    public static final String LANDING_URL_KEY = DOI_KEY + ".landingUrl";
    public static final String DATACITE_MDS_URL_KEY = DOI_KEY + ".datacite.mdsUrl";
    public static final String DATACITE_MDS_USERNAME_KEY = DOI_KEY + ".datacite.username";
    public static final String DATACITE_MDS_PASSWORD_KEY = DOI_KEY + ".datacite.password";
    public static final String DATACITE_ACCOUNT_PREFIX_KEY = DOI_KEY + ".datacite.accountPrefix";
    public static final String DOI_IDENTIFIER_PREFIX_KEY = DOI_KEY + ".doiIdentifierPrefix";

    // optional properties
    public static final String RANDOM_TEST_ID_KEY = DOI_KEY + ".randomTestID";

    //Alternative DOI settings properties
    public static final String PUBLISHER_GROUP_URI_KEY = DOI_KEY + ".publisherGroupURI";

    @Override
    public void doInit() {
        getConfig(true);
        checkParentFolders();
    }

    public static MultiValuedProperties getConfig() {
        return getConfig(false);
    }

    public static VOSURI getParentVOSURI(MultiValuedProperties props) {
        String parentUri = props.getFirstPropertyValue(VOSPACE_PARENT_URI_KEY);
        VOSURI vosURI;
        try {
            vosURI = new VOSURI(parentUri);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("invalid DOI parent VOSpace URI: " + parentUri);
        }
        return vosURI;
    }

    public static String getDoiIdentifierPrefix(MultiValuedProperties props) {
        String doiIdentifierPrefix = props.getFirstPropertyValue(DoiInitAction.DOI_IDENTIFIER_PREFIX_KEY);
        return Objects.requireNonNullElse(doiIdentifierPrefix, "");
    }

    public static GroupURI getPublisherGroupURI(MultiValuedProperties props) {
        String publisherGroupURI = props.getFirstPropertyValue(PUBLISHER_GROUP_URI_KEY);
        return publisherGroupURI == null ? null : new GroupURI(URI.create(publisherGroupURI));
    }

    private static MultiValuedProperties getConfig(boolean verify) {
        PropertiesReader reader = new PropertiesReader("doi.properties");
        MultiValuedProperties props = reader.getAllProperties();
        StringBuilder sb = new StringBuilder();
        Boolean ok = true;

        // required properties
        checkStringKey(props, sb, ok, true, METADATA_PREFIX_KEY);

        checkStringKey(props, sb, ok, true, DOI_IDENTIFIER_PREFIX_KEY);
        checkStringKey(props, sb, ok, true, DATACITE_MDS_USERNAME_KEY);
        checkStringKey(props, sb, ok, true, DATACITE_MDS_PASSWORD_KEY);
        checkStringKey(props, sb, ok, true, DATACITE_ACCOUNT_PREFIX_KEY);
        checkVOSURIKey(props, sb, ok, verify, VOSPACE_PARENT_URI_KEY);
        checkURLKey(props, sb, ok, verify, LANDING_URL_KEY);
        checkURLKey(props, sb, ok, verify, DATACITE_MDS_URL_KEY);

        // optional properties
        checkStringKey(props, sb, ok, false, DOI_GROUP_PREFIX_KEY);
        checkStringKey(props, sb, ok, false, RANDOM_TEST_ID_KEY);

        // alternative properties
        checkStringKey(props, sb, ok, false, PUBLISHER_GROUP_URI_KEY);

        if (!ok) {
            throw new IllegalStateException("incomplete config: " + sb);
        }
        return props;
    }

    private static void checkStringKey(MultiValuedProperties props, StringBuilder sb, Boolean ok, boolean required, String key) {
        String value = props.getFirstPropertyValue(key);
        sb.append(String.format("\n\t%s: ", key));
        if (value == null) {
            sb.append("MISSING");
            if (required) {
                ok = false;
            }
        } else {
            sb.append("OK");
        }
    }

    private static void checkURLKey(MultiValuedProperties props, StringBuilder sb, Boolean ok, boolean verify, String key) {
        String value = props.getFirstPropertyValue(key);
        sb.append(String.format("\n\t%s: ", key));
        if (value == null) {
            sb.append("MISSING");
            ok = false;
        } else if (verify) {
            try {
                new URL(value);
                sb.append("OK");
            } catch (MalformedURLException e) {
                sb.append("INVALID URL");
                ok = false;
            }
        } else {
            sb.append("OK");
        }
    }

    private static void checkVOSURIKey(MultiValuedProperties props, StringBuilder sb, Boolean ok, boolean verify, String key) {
        String value = props.getFirstPropertyValue(key);
        sb.append(String.format("\n\t%s: ", key));
        if (value == null) {
            sb.append("MISSING");
            ok = false;
        } else if (verify) {
            try {
                new VOSURI(value);
                sb.append("OK");
            } catch (URISyntaxException e) {
                sb.append("INVALID VOSPACE URI: ").append(e.getMessage());
                ok = false;
            }
        } else {
            sb.append("OK");
        }
    }

    // check that the DOI parent node uri, configured with the VOSPACE_PARENT_URI_KEY property,
    // exists and has the expected properties.
    private static void checkParentFolders() {

        Subject adminSubject = SSLUtil.createSubject(new File(System.getProperty("user.home") + "/.ssl/doiadmin.pem"));
        adminSubject = AuthenticationUtil.augmentSubject(adminSubject);
        String adminUsername = getUsername(adminSubject);

        MultiValuedProperties config = getConfig();
        VOSURI parentVOSURI = getParentVOSURI(config);
        URI vospaceResourceID = parentVOSURI.getServiceURI();
        String parentPath = parentVOSURI.getPath();
        VOSpaceClient vosClient = new VOSpaceClient(vospaceResourceID);

        Node node;
        try {
            node = vosClient.getNode(parentPath);
        } catch (ResourceNotFoundException e) {
            throw new IllegalStateException(String.format("DOI parent node %s not found", parentPath));
        } catch (Exception e) {
            throw new IllegalStateException(String.format("DOI parent node %s error because %s", parentPath, e.getMessage()));
        }

        // confirm it's a ContainerNode
        if (!(node instanceof ContainerNode)) {
            throw new IllegalStateException(String.format("DOI parent node %s is not a ContainerNode", parentPath));
        }
        ContainerNode containerNode = (ContainerNode) node;

        // check node owner
        String ownerID = containerNode.ownerDisplay;
        if (!adminUsername.equals(ownerID)) {
            throw new IllegalStateException(String.format("DOI parent node %s owner %s doesn't match configured admin user %s",
                    parentPath, ownerID, adminUsername));
        }

        // check node has public access
        if (!containerNode.isPublic) {
            throw new IllegalStateException(String.format("DOI parent node %s must have isPublic set to true", parentPath));
        }

        // check inheritPermissions is true
        if (!containerNode.inheritPermissions) {
            throw new IllegalStateException(String.format("DOI parent node %s must have inheritPermissions set to true", parentPath));
        }
    }

    private static String getUsername(Subject subject) {
        Set<Principal> principals = subject.getPrincipals();
        for (Principal principal : principals) {
            if (principal instanceof HttpPrincipal) {
                HttpPrincipal httpPrincipal = (HttpPrincipal) principal;
                return httpPrincipal.getName();
            }
        }
        throw new IllegalStateException(String.format("no HttpPrincipal found for %s", subject));
    }

}
