BStore : Store {classvar <playPath, <samplerPath, <>playFolder=0, <>playFormat=\audio;
	classvar <>samplerFormat=\audio, <>diskStart=0, <>diskBufSize=1, <>cueCount=1, <>allocCount=1;

	*addRaw {arg type, format, settings, function, playChans;
		var path, boolean, typeStore, existFormat, bstoreIDs;
		bstoreIDs = this.bstoreIDs;
		case
		{type == \play} {
			path = this.getPlayPath(format);
		}
		{type == \sampler} {
			path = this.getSamplerPath(format, settings);
		}
		{type == \alloc} {
			path = settings;
		}
		{type == \cue} {
			path = this.getPlayPath(\audio);
		};
		case
		{type == \play} {
			if(settings.isArray.not, {
			typeStore = this.addPlay(settings, path, {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, settings);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			});
			}, {
				typeStore = this.addPlay(settings[0], path, {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, settings);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			}, settings[1]);
			});
		}
		{type == \sampler} {
			typeStore = this.addSampler(settings, path, {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, settings);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			});
		}
		{type == \alloc} {
			if(bstoreIDs.notNil, {
			existFormat = bstoreIDs.flop[1].indexOf(format);
			});
			if(existFormat.notNil, {
				typeStore = this.buffByID(bstoreIDs[existFormat]);
				("//Buffer already been used as: ~buffer" ++
					typeStore.bufnum).radpost;
			}, {
			typeStore = this.addAlloc(path, function: {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, path);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			});
			});
		}
		{type == \cue} {
			if(bstoreIDs.notNil, {
			existFormat = bstoreIDs.flop[1].indexOf(format);
			});
			if(existFormat.notNil, {
				typeStore = this.buffByID(bstoreIDs[existFormat]);
				("//Buffer already been used as: ~buffer" ++
					typeStore.bufnum).radpost;
			}, {
			typeStore = this.addCue(settings, path, function: {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, settings);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			});
				});
		};
	}

	*add {arg type, settings, function;
		var format, newSettings;

		case
		{(type == \play)} {
			format = playFormat;
			newSettings = settings;
		}
		{type == \sampler} {
			format = samplerFormat;
			newSettings = settings;
		}
		{type == \alloc} {
			format = settings[0];
			newSettings = settings.copyRange(1,3);
		}
		{type == \cue} {
			format = settings[0];
			newSettings = settings[1];
		};

		this.addRaw(type, format, newSettings, function);
	}

	*addAll {arg array, function;
		var arr, cond;
		cond = Condition(false);
		{
			array.do{|item|
				cond.test = false;
				BStore.addRaw(item[0], item[1], item[2], {|buf|
					arr = arr.add(buf);
					cond.test = true;
					cond.signal;
				});
				cond.wait;
			};
			function.(arr);
		}.fork;
	}

	*remove {arg type, format, settings;
		var bstoreIDs, bstoreIndex, bstores, thisBStore, freeBufArr;
		bstoreIDs = this.bstoreIDs;
		bstores = this.bstores;
		if(type == \alloc, {
			bstoreIndex = bstoreIDs.flop[1].indexOfEqual(format);
		}, {
			bstoreIndex = bstoreIDs.indexOfEqual([type, format, settings]);
		});
		if(bstoreIndex.notNil, {
			thisBStore = bstores[bstoreIndex];

			case
			{(type == \play).or(type == \alloc).or(type == \cue)} {
				BufferSystem.freeAt(BufferSystem.bufferArray.indexOf(thisBStore));
				this.removeAt(bstoreIndex);

			}
			{type == \sampler} {
				thisBStore.do{|item|
					if((bstores.flat.indicesOfEqual( item).size > 1).not, {
						freeBufArr = freeBufArr.add(BufferSystem.bufferArray.indexOf(item));
					});
				};
				BufferSystem.freeAtAll(freeBufArr.sort);
				this.removeAt(bstoreIndex);
			}
		});
	}

	*removeID {arg ids;
		var currentIDs;
		currentIDs = this.bstoreIDs;
		if(currentIDs.notNil, {
		if(currentIDs.indexOfEqual(ids).notNil, {
		this.remove(ids[0], ids[1], ids[2]);
		});
		});
	}

	*removeAll {
		this.removeBStores;
	}

	*removeByIndex {arg index;
		var ids;
		ids = this.bstoreIDs[index];
		if(ids.notNil, {
			this.remove(ids[0], ids[1], ids[2]);
		});
	}

	*removeIndices {arg indices;
		var count=0;
		indices.do{|item|
				this.removeByIndex(item-count);
				count = count+1;
			}
	}

	*removeByArg {arg argument, index;
		var indices, count=0;
		indices = this.bstoreIDs.flop[index].indicesOfEqual(argument);
		if(indices.notNil, {
			this.removeIndices(indices);
		});
	}

	*removeByType {arg type;
		this.removeByArg(type, 0);
	}

	*removeByFormat {arg format;
		this.removeByArg(format, 1);
	}

	*removeBySetting {arg setting;
		this.removeByArg(setting, 2);
	}

	*getDirPath {arg format=\audio, directory, subDir;
		var folderPath, fileIndex, selectedPath;
		this.new;
		playPath = this.mainPath ++ directory;

		if([\audio, \scpv].includes(format), {
			if(format == \audio, {
				folderPath = (playPath ++ subDir.asString);
			}, {
				folderPath = (playPath ++ "scpv/" ++ subDir.asString);
			});

		}, {
			"not a recognized audio format".warn;
		});

		^folderPath.asString;
	}

	*getPlayPath {arg format=\audio, fileName=\test;
		^this.getDirPath(format, "SoundFiles/Play/", playFolder);
	}

	*getSamplerPath {arg format=\audio, samplerName=\str;
		^this.getDirPath(format, "SoundFiles/Sampler/", "");
	}

	*addPlay {arg settings, path, function, playChans;
		^BufferSystem.add(settings, path, function, playChans);
	}

	*addSampler {arg settings, path, function;
		var samplerArr;
		if(DataFile.read(\sampler).includes(settings), {
			samplerArr = DataFile.read(\sampler, settings);
			^BufferSystem.addAll(samplerArr, path, function);
		}, {
			"Sampler not found".warn;
		});
	}

	*addAlloc {arg settings, function;
		^BufferSystem.add(settings[0], settings[1], function, settings[2]);
	}

	*addCue {arg settings, path, function;
		^BufferSystem.add([settings, 'cue', [diskStart,diskBufSize]], path, function);
	}

	*buffByArg {arg argument, index;
		var indices, buffs, result, count=0;
		indices = this.bstoreIDs.flop[index].indicesOfEqual(argument);
		if(indices.notNil, {
			buffs = this.bstores.atAll(indices);
			if(buffs.size == 1, {
				result = buffs[0];
			}, {
				result = buffs;
			});
			^result;
		});
	}

	*buffByType {arg type;
		^this.buffByArg(type, 0);
	}

	*buffByFormat {arg format;
		^this.buffByArg(format, 1);
	}

	*buffBySetting {arg setting;
		^this.buffByArg(setting, 2);
	}

	*buffByID {arg bstoreID;
		^this.bstores[this.bstoreIDs.indexOfEqual(bstoreID)];
	}

	*bstoreTags {
		var newArr;
		this.bstoreIDs.do{|item| newArr = newArr.add(item.flat)};
		^newArr;
	}

	*buffByTag {arg bstoreID;
		^this.bstores[this.bstoreTags.indexOfEqual(bstoreID)];
	}

		*setBufferID {arg buffer, blockFuncString;
		var storeType, bufferID;
		case
		{buffer.isNumber} {
			storeType = \alloc;
			buffer = [(\alloc++allocCount).asSymbol, buffer];
			bufferID = [storeType, buffer[0], [buffer[1]] ];
			allocCount = allocCount + 1;
		}
		{buffer.isSymbol} {
			case
			{(blockFuncString.find("PlayBuf.ar(")).notNil} {
				storeType = \play;
				BStore.playFormat = \audio;
				bufferID = [storeType, \audio, buffer].flat;
			}
			{blockFuncString.find("PV_PlayBuf").notNil} {
				storeType = \play;
				BStore.playFormat = \scpv;
				bufferID = [storeType, \scpv, buffer];
			}
			{blockFuncString.find("DiskIn.ar(").notNil} {
				storeType = \cue;
				buffer = [(\cue++cueCount).asSymbol, buffer].flat;
				bufferID = [storeType, buffer].flat;
				cueCount = cueCount + 1;
			};
		};
		^[storeType, buffer, bufferID];
	}

	*buffAsString {arg buffer;
		var bufIDs, bufIndex, bufString;
		bufIDs = this.buffByID(buffer);
		bufIndex = BufferSystem.bufferArray.indexOf(bufIDs);
		bufString = BufferSystem.globVarArray[bufIndex];
		^bufString;
	}

}