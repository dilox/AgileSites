package $site;format="normalize"$.element;

import static wcs.core.Common.*;
import wcs.core.Log;
import wcs.core.Index;
import wcs.core.Asset;
import wcs.core.Model;
import wcs.java.Picker;
import wcs.java.AssetSetup;
import wcs.java.CSElement;
import wcs.java.SiteEntry;
import wcs.java.Element;
import wcs.java.Env;

@Index("$site;format="normalize"$/elements.txt")
public class Wrapper extends Element {
	
	private final static Log log = Log.getLog(Wrapper.class);

	public static AssetSetup setup() {
		return new CSElement("$site$_Wrapper", $site;format="normalize"$.element.Wrapper.class,
				new SiteEntry("$site;format="normalize"$", true, "$site$/$site$_Wrapper"));
	}

	@Override
	public String apply(Env e) {
		log.trace("$site$ Wrapper");

		Picker html = Picker.load("/$site;format="normalize"$/simple.html");

		// change relative references to absolute
		html.prefixAttrs("link[rel=stylesheet]", "href", "/cs/$site;format="normalize"$/");
		html.prefixAttrs("script", "src", "/cs/$site;format="normalize"$/");

		// handle generic errors
		if (e.isVar("error")) {			
			Model m = model(arg("name", "Error"), arg("description", e.getString("error"))); 
			return html.replace("#content", e.call("$site$_Error",// 
							arg("error", e.getString("error"))))//
					/*.dump(log)*/.outerHtml(m);
		}

		// handle asset not found
		Asset a = e.getAsset();
		if (a == null) {
			String error = "Asset not found";
			Model m = model(arg("name", "Error"), arg("description", error));
			return html.replace("#content", //
					e.call("$site$_Error",arg("error", error)))//
					/*.dump(log)*/.outerHtml(m);
		}

		// render the asset using his default template
		Model m = model(arg("name",a.getName()), arg("description", a.getDescription())); 
		html.replace("#content", a.call(a.getTemplate()));
		return html/*.dump(log)*/.outerHtml(m);
	}
}
