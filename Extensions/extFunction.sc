+ Function {

	patternToPdef {
		var cs, patType, argNames, part2, part1, fcs;
		cs = this.cs;
		part2 = cs.copyRange(cs.find("Pbind"), cs.size);
		part1 = cs.copyRange(0, cs.find("Pbind")-1);
		argNames = this.argNames;
		patType = Pseq;
		argNames.do{|item|
			var string;
			string = ("Pdefn('" ++ argNames[0] ++ "_" ++ item ++ "', " ++ patType.asString ++ "(" ++
				item ++ ", inf))").asString;
			part2 = part2.replace(item.asString, string);
		};
		fcs = part1 ++ "Pdef(key).quant = quant;" ++ 10.asAscii ++ "Pdef(key, " ++ part2;
		fcs = fcs.replace("}", ");}");
		^fcs.interpret;
	}

	toSynthDef {arg key=\bla, post=true;
		var arr, arr2, string, string2, synthDef;
		this.argNames.do{|item| arr = arr.add(item) };
		string = arr.asString.replace("[", "").replace("]", "");
		arr.do{|item, index| arr2 = arr2.add(item ++ "=" ++ this.defaultArgs[index].cs) };
		string2 = arr2.asString.replace("[", "").replace("]", "");
		synthDef = "SynthDef(" ++ key.cs ++ ", {arg out=0," ++ string2 ++ "; var output;" ++
		10.asAscii ++ "output = Out.ar(out, " ++ this.cs ++ ".(" ++ string ++ "))});";
		if(post, {synthDef.postln});
		^synthDef.interpret;
	}

	spec {
		var string, stringFunc, indecesFind, indecesBrack, argNames, arrArgs, indArgs;
		var indecesClose, cuts, stringSpecs, specArr, newString, indecesArgs;
		argNames = this.argNames;
		string = this.cs;
		if(string.includesString("->"), {
		stringFunc = string.replace("-> [", "->[");
		indecesFind = stringFunc.findAll("->[");
		indecesArgs = argNames.collect{|item| stringFunc.find(item.asString); };
		indArgs = indecesFind.collect{|item|
			indecesArgs.indexOf(indecesArgs.reject{|it| item < it}.last);
		};
		arrArgs = argNames.atAll(indArgs);
		if(indecesFind.notNil, {
			indecesBrack = stringFunc.findAll("]");
			indecesClose = indecesFind.collect{|it|
				indecesBrack.select{|item| item > it }.first;
			};
			cuts = ([indecesFind] ++ [indecesClose]).flop;
			stringSpecs = cuts.collect{|item| stringFunc.copyRange(item[0], item[1]) };
			specArr = stringSpecs.collect{ |item| item.replace("->", "").interpret };
			specArr = [arrArgs] ++ [specArr];
			specArr = specArr.flop;
			specArr = specArr.collect{|item|
				var func, specs, out;
				func = item[1].select{|item| item.isFunction };
				specs = item[1].reject{|item| item.isFunction };
				if(func[0].isNil, {
					out = [item[0], specs];
				}, {
					out = [item[0], specs, func[0]];
				});
				out;
			};
			newString = stringFunc;
			stringSpecs.do{|item|
				newString = 	newString.replace(item);};
		}, {
			newString = 	stringFunc;
			specArr = [];
		});
		^[newString.interpret, specArr];
		}, {
			^[this, nil];
		});
	}

	specFunc {
		^this.spec[0];
	}

	specArr {
		^this.spec[1];
	}

	filterFunc {arg ndefin, audio=true;
		var out, string, funcCS, ndefout;
		string = 92.asAscii ++ "in";
		funcCS = this.cs;
		if(funcCS.find(string ++ "1").isNil, {
			if(ndefin.isArray.not, {
				if(audio, {
					ndefout = "Ndef.ar(" ++ ndefin.key.cs ++ ", " ++ ndefin.numChannels ++ ")";
				});
				out = funcCS.replace(string, ndefout );
			}, {
				if(audio, {
					ndefout = ndefin.collect({|item|
						"Ndef.ar(" ++ item.key.cs ++ ", " ++ item.numChannels ++ ")";
					});
				}, {ndefout = ndefin});
				out = funcCS.replace(string, ndefout.cs.replace("\"", "") ++ ".sum" );
			});
		}, {
			if(ndefin.isArray, {
				out = funcCS;
				if(audio, {
					ndefout = ndefin.collect({|item|
						"Ndef.ar(" ++ item.key.cs ++ ", " ++ item.numChannels ++ ")";
					});
				}, {ndefout = ndefin});
				ndefout.do{|item, index|
					out = out.replace(string ++ (index+1).cs, item );
				};
			}, {
				"incorrect input, should be an array of ndefs".warn;
			});
		});
		^out.interpret;
	}

}