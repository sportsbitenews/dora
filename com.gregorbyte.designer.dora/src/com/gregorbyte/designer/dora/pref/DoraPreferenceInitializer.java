package com.gregorbyte.designer.dora.pref;

import org.eclipse.jface.preference.IPreferenceStore;

import com.gregorbyte.designer.dora.Activator;
import com.ibm.designer.domino.preferences.AbstractDominoPreferenceInitializer;

public class DoraPreferenceInitializer extends
		AbstractDominoPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		IPreferenceStore store =  Activator.getDefault().getPreferenceStore();

		store.setDefault("dora", "isgood");
		

	}

}
