// This file was auto-generated by the veyron vdl tool.
// Source: identity.vdl
package com.veyron.services.identity;

/**
 * OAuthBlesser exchanges OAuth authorization codes OR access tokens for
 * an email address from an OAuth-based identity provider and uses the email
 * address obtained to bless the client.
 * 
 * OAuth is described in RFC 6749 (http:tools.ietf.org/html/rfc6749),
 * though the Google implementation also has informative documentation at
 * https:developers.google.com/accounts/docs/OAuth2
 * 
 * WARNING: There is no binding between the channel over which the
 * authorization code or access token was obtained (typically https)
 * and the channel used to make the RPC (a veyron virtual circuit).
 * Thus, if Mallory possesses the authorization code or access token
 * associated with Alice's account, she may be able to obtain a blessing
 * with Alice's name on it.
 * 
 * TODO(ashankar,toddw): Once the "OneOf" type becomes available in VDL,
 * then the "any" should be replaced by:
 * OneOf<wire.ChainPublicID, []wire.ChainPublicID>
 * where wire is from:
 * import "veyron2/security/wire"
 */

public interface OAuthBlesser  {

    
    

    
    // BlessUsingAuthorizationCode exchanges the provided authorization code
// for an access token and then uses that access token to obtain an
// email address.
//
// The redirect URL used to obtain the authorization code must also
// be provided.

    public java.lang.Object blessUsingAuthorizationCode(final com.veyron2.ipc.Context context, final java.lang.String authcode, final java.lang.String redirecturl) throws com.veyron2.ipc.VeyronException;
    public java.lang.Object blessUsingAuthorizationCode(final com.veyron2.ipc.Context context, final java.lang.String authcode, final java.lang.String redirecturl, final com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException;

    
    

    
    // BlessUsingAccessToken uses the provided access token to obtain the email
// address and returns a blessing.

    public java.lang.Object blessUsingAccessToken(final com.veyron2.ipc.Context context, final java.lang.String token) throws com.veyron2.ipc.VeyronException;
    public java.lang.Object blessUsingAccessToken(final com.veyron2.ipc.Context context, final java.lang.String token, final com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException;

}
