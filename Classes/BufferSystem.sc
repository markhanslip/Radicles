BufferSystem {classvar condition, server, <bufferArray, <tags, countTag=0;
	classvar <bufAlloc, <>defaultPath, <>postWhere=\ide, <>postWin;

	*new {
		condition = Condition.new;
		server = Server.default;
	}

	*read {arg pathName, function;
		var main, buffer;
		main = this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					buffer = Buffer.read(server, pathName).postin(postWhere, \ln, postWin);
					bufferArray = bufferArray.add(buffer);
					tags = tags.add(this.pathToTag(pathName));
					server.sync(condition);
					bufAlloc = false;
					function.(buffer);
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*readAll {arg pathArr, function;
		var main, buffer, returnArray;
		main = this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					pathArr.do{|item|
						buffer = Buffer.read(server, item).postin(postWhere, \ln, postWin);
						bufferArray = bufferArray.add(buffer);
						tags = tags.add(this.pathToTag(item));
						returnArray = returnArray.add(buffer);
						server.sync(condition);
					};
					function.(returnArray);
					bufAlloc = false;
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*readDir {arg path, function;
		var myPath, newArr;
		if(path.notNil, {
			myPath = PathName.new(path);
			myPath.files.do{|item| newArr = newArr.add(item.fullPath)};
			this.readAll(newArr, function);
		}, {
			"Not path specified".warn;
		});
	}

	*bufferInfo {var array, tag, count=0;
		bufferArray.do{|item|
			if(item.path.notNil, {
				/*myPath = PathName.new(item.path);
				tag = myPath.fileNameWithoutExtension.asSymbol;*/
				tag = this.pathToTag(item.path);
			}, {
				tag = ("alloc" ++ count).asSymbol;
				count = count + 1;
			});
			array = array.add([tag, item.numChannels, item.bufnum,
				item.numFrames, item.sampleRate, item.path]);
		};
		^array;
	}

	*bufferPaths {var arr;
		bufferArray.do{|item|
			if(item.path.notNil, {
				arr = arr.add([item.path, item]);
			});
		};
		^arr;
	}

	*getPath {arg fileName=\test, pathDir;
		var folderPath, fileIndex, selectedPath;
		var myPath, newArray, newerArr;
		myPath = PathName.new(pathDir);
		myPath.files.do{|item| newArray = newArray.add(item.fileNameWithoutExtension)};

		newArray.do{|item, index|
			if(item.asSymbol == fileName, {
				fileIndex = index;
			});
		};

		if(fileIndex.notNil, {
			myPath = PathName.new(pathDir);
			myPath.files.do{|item| newerArr = newerArr.add(item.fullPath)};
			selectedPath = newerArr[fileIndex]
		}, {
			"Incorrect fileName, soundfile does not exist".warn;
		});

		^selectedPath;
	}

	*alloc {arg numFrames, numChannels, function;
		var main, buffer;
		main = this.new;

		numFrames ?? {numFrames = 44100};
		numChannels ?? {numChannels = 1};

		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					buffer = Buffer.alloc(server, numFrames, numChannels).postin(postWhere, \ln, postWin);
					bufferArray = bufferArray.add(buffer);
					tags = tags.add( ("alloc" ++ countTag).asSymbol );
					countTag = countTag + 1;
					server.sync(condition);
					bufAlloc = false;
					function.(buffer);
				}.fork;
			});
		}, {
			"Server is not running".warn;
		});

	}

	*allocAll {arg argArr, function;
		var main, buffer, returnArr;
		main = this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;

					argArr.do{|item|
						item[0] ?? {item[0] = 44100};
						item[1] ?? {item[1] = 1};

						buffer = Buffer.alloc(server, item[0], item[1])
						.postin(postWhere, \ln, postWin);
						bufferArray = bufferArray.add(buffer);
						tags = tags.add( ("alloc" ++ countTag).asSymbol );
						countTag = countTag + 1;
						returnArr = returnArr.add(buffer);
						server.sync(condition);

					};
					bufAlloc = false;
					function.(returnArr);

				}.fork;
			});
		}, {
			"Server is not running".warn;
		});
	}

	*cue {arg pathName, startFrame=0, bufSize=1, function;
		var main, buffer, file, chanNum;
		main = this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					file = SoundFile.new;
					file.openRead(pathName);
					chanNum = file.numChannels;
					file.close;
					buffer = Buffer.cueSoundFile(server, pathName, startFrame, chanNum,
						32768*bufSize, function).postin(postWhere, \ln, postWin);
					bufferArray = bufferArray.add(buffer);
					tags = tags.add(this.pathToTag(pathName));
					server.sync(condition);
					bufAlloc = false;
					function.(buffer);
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*cueAll {arg arr, function;
		var main, buffer, returnArray, file, chanNum, newArr;
		main = this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					arr.do{|item|
						file = SoundFile.new;
						file.openRead(item[0]);
						chanNum = file.numChannels;
						file.close;
						if(item[1].notNil, {
							newArr = item[1];
						}, {
							newArr = [0,1];
						});
						buffer = Buffer.cueSoundFile(server, item[0], newArr[0], chanNum,
							32768*newArr[1]).postin(postWhere, \ln, postWin);
						bufferArray = bufferArray.add(buffer);
						tags = tags.add(this.pathToTag(item[0]));
						returnArray = returnArray.add(buffer);
						server.sync(condition);
					};
					function.(returnArray);
					bufAlloc = false;
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*add {arg arg1, arg2, function;
		var getPath, getIndex, getBufferPaths, cueBool;
		if(arg1.isNumber, {
			//allocate buffer: arg1: frames, arg2: channels
			this.alloc(arg1, arg2, function)
		}, {
			//read buffer: arg1: fileName, arg2: pathDir
			arg2 ?? {arg2 = defaultPath};

			if(arg2.notNil, {
				cueBool = arg1.isArray.not;
				if(cueBool, {
					getPath = this.getPath(arg1, arg2);
				}, {
					getPath = this.getPath(arg1[0], arg2);
				});

				if(getPath.notNil, {

					getBufferPaths = this.bufferPaths;

					if(getBufferPaths.notNil, {
						getIndex = getBufferPaths.flop[0].indexOfEqual(getPath);
						if(getIndex.isNil, {
							if(cueBool, {
								this.read(getPath, function);
							}, {
								this.cue(getPath, arg1[1][0], arg1[1][1], function);
							});
						}, {
							"File already allocated as: ".postin(postWhere, \ln, postWin);
							function.(getBufferPaths.flop[1][getIndex]
								.postin(postWhere, \ln, postWin) );
						});
					}, {
						if(cueBool, {
							this.read(getPath, function);
						}, {
							if(arg1[1].notNil, {
							this.cue(getPath, arg1[1][0], arg1[1][1], function);
							}, {
							this.cue(getPath, function: function);
							});
						});
					});
				});
			}, {
				"No path selected".warn;
			});
		});
	}

	*addAllPaths {arg arr, path, function;
		var getPath, getIndex, getBufferPaths, pathArr, stringArr, existingBuffArr, finalArr;
		var cueBool, cueArgs;
		if(arr.flat[0].isNumber, {
			this.allocAll(arr, function)
		}, {
			if(path.notNil, {
				arr.do{|item, index|

					cueBool = item.isArray.not;

				if(cueBool, {
					getPath = this.getPath(item, path[index]);
				}, {
					getPath = this.getPath(item[0], path[index]);
						cueArgs = cueArgs.add([getPath, item[1]]);
				});

					if(getPath.notNil, {

						getBufferPaths = this.bufferPaths;

						if(getBufferPaths.notNil, {
							getIndex = getBufferPaths.flop[0].indexOfEqual(getPath);
							if(getIndex.isNil, {
								pathArr = pathArr.add(getPath);
							}, {
								pathArr = pathArr.add(getBufferPaths.flop[1][getIndex];);
							});
						}, {
							pathArr = pathArr.add(getPath);
						});
					})
				};

				pathArr.do{|item|
					if(item.isString, {
						stringArr = stringArr.add(item);
					}, {
						existingBuffArr = existingBuffArr.add(item);
						"File already allocated as: ".postin(postWhere, \ln, postWin);
						item.postin(postWhere, \ln, postWin);
					});
				};
				if(stringArr.notNil, {

					if(cueArgs.isNil, {
							this.readAll(stringArr, {
						finalArr = arr.collect{|tag|	this.get(tag)};
						function.(finalArr);
					});
						}, {
							this.cueAll(cueArgs, {
						finalArr = arr.flop[0].collect{|tag|
								this.get(tag)
							};
						function.(finalArr);
					});
						});

				}, {
					function.(existingBuffArr);
				});

			}, {
				"No path specified".warn;
			});
		});
	}

	*addAll {arg arr, path, function;
		if(arr.flat[0].isNumber, {
			//allocate arr: [frames, channels]
			this.addAllPaths(arr, function: function)
		}, {
			path ?? {path = defaultPath};
			if(path.notNil, {
				this.addAllPaths(arr, path!arr.size, function);
			}, {
				"No path specified".warn;
			});
		});
	}

	*addPairs {arg arr, function;
		var newArr;
		if(arr.indexOf(nil).notNil.not, {
			if(arr.flat[0].isNumber, {
				//allocate arr: [frames, channels]
				this.addAllPaths(arr.clump(2), function: function)
			}, {
				newArr = arr.clump(2).flop;
				this.addAllPaths(newArr[0], newArr[1], function);
			});
		}, {
			"Not all infomation is specified".warn;
		});
	}

	*addDir {arg path, function;
		var myPath, newArr;
		path ?? {path = defaultPath};
		if(path.notNil, {
			myPath = PathName.new(path);
			myPath.files.do{|item|
				newArr = newArr.add(item.fileNameWithoutExtension.asSymbol;
			)};
			this.addAll(newArr, path, function);
		}, {
			"Not path specified".warn;
		});
	}

	*cueDir {arg path, startFrame=0, bufSize=1, function;
		var myPath, newArr;
		path ?? {path = defaultPath};
		if(path.notNil, {
			myPath = PathName.new(path);
			myPath.files.do{|item|
				newArr = newArr.add(
					[item.fileNameWithoutExtension.asSymbol, [startFrame, bufSize]]
			)};
			this.addAll(newArr, path, function);
		}, {
			"Not path specified".warn;
		});
	}

	*addAllTypes {arg arr, path, function;
		var bufs, files, cues, finalArr, condition;
		var indexSort, sortIndex, newIndexArr;

		bufs=[];
		files=[];
		cues=[];

		arr.do{|bufInfo|
			if(bufInfo.isArray, {
				if(bufInfo[0].isNumber, {
				bufs = bufs.add(bufInfo);
				}, {
					cues = cues.add(bufInfo);
				});
			}, {
				files = files.add(bufInfo);
			});
		};

arr.do{|it|
	if(it.isArray, {
		if(it[0].isNumber, {
			indexSort = indexSort.add(0);
		}, {
			indexSort = indexSort.add(2);
		});
	}, {
		indexSort = indexSort.add(1);
	});
};

indexSort.do{|it,in| sortIndex = sortIndex.add([it,in]) };
sortIndex.sort{ arg a, b; a[0] <= b[0] };

		{
			condition = Condition(false);
	if(bufs.notEmpty, {
			BufferSystem.addAll(bufs, function: {|item|
					finalArr = finalArr.add(item);
					condition.test = true;
					condition.signal;
				});
		});
		condition.wait;
			condition.test = false;
	if(files.notEmpty, {
			BufferSystem.addAll(files, path, {|item|
					finalArr = finalArr.add(item);
					condition.test = true;
					condition.signal;
				});
		});
		condition.wait;
			condition.test = false;
	if(cues.notEmpty, {
			BufferSystem.addAll(cues, path, {|item|
					finalArr = finalArr.add(item);
					condition.test = true;
					condition.signal;

				});
		});
			condition.wait;
			finalArr = finalArr.flat;
			newIndexArr = Array.fill(sortIndex.size, nil);
			sortIndex.flop[1].do{|item, index| newIndexArr[item] = finalArr[index] };
			function.(newIndexArr.flat);

	}.fork;

	}

	*fileNames {var tagArr;
		if(bufferArray.notNil, {
			bufferArray.do{ |item|
				var filePath, allocTag;
				filePath = item.path;
				if(filePath.notNil, {
					tagArr = tagArr.add(PathName(filePath).fileName.asSymbol);
					/*function.(filePath).asSymbol;*/
				});
			};
			if(tagArr.notNil, {
				^tagArr;
			}, {
				"No files allocated".warn;
			});
		}, {
			"No buffers allocated".warn;
		});
	}

	*get {arg tag;
		var resultBuf, bufIndex, symbols;
		if(bufferArray.notNil, {
			symbols = this.tags;
			bufIndex = symbols.indexOfEqual(tag);
			if(bufIndex.notNil, {
				resultBuf = bufferArray[bufIndex];
			}, {
				"Tag not found".warn;
			});
		}, {
			"No buffers allocated".warn;
		});
		^resultBuf;
	}

	*getFile {arg string;
		var resultBuf;
		if(bufferArray.notNil, {
			bufferArray.do{|item|
				if(item.path.notNil, {
					if(string == item.path.basename, {resultBuf = item});
				});
			};
		}, {
			"No buffers allocated".warn;
		});
		^resultBuf;
	}

	*getFromPath {arg string;
		var resultBuf;
		if(bufferArray.notNil, {
			bufferArray.do{|item|
				if(item.path.notNil, {
					if(string == item.path, {resultBuf = item});
				});
			};
		}, {
			"No buffers allocated".warn;
		});
		^resultBuf;
	}

	*arrDir {
		^bufferArray.collect{|item| item.path.dirname }.rejectSame;
	}

	*bufferByDir {var indexArr, indexShape;
		this.arrDir.do{|subdir, index|
			bufferArray.do{|buf|
				if(buf.path.dirname == subdir, {indexArr = indexArr.add(index)});
			}
		};
		(indexArr.last+1).do{|item|	indexShape = indexShape.add(indexArr.indicesOfEqual(item) ); };
		^bufferArray.reshapeLike(indexShape);
	}

	*readSubDirs {arg path, function;
		var fullPaths;
		path ?? {path = defaultPath};
		PathName(path).entries.do{|subfolder|
			subfolder.entries.do{|file| fullPaths = fullPaths.add(file.fullPath) };
		};
		if(fullPaths.notNil, {
			this.readAll(fullPaths, { function.(this.bufferByDir); });
		}, {
			"No subdirectories in this directory".warn;
		});
	}

	*addSubDirs {arg path, function;
		var arr;
		path ?? {path = defaultPath};
		PathName(path).entries.do{|subfolder|
			subfolder.entries.do{|file|
				arr = arr.add([file.fileNameWithoutExtension.asSymbol, file.fullPath.dirname]) };
		};
		if(arr.notNil, {
			this.addAllPaths(arr.flop[0], arr.flop[1], { function.(this.bufferByDir); });
		}, {
			"No subdirectories in this directory".warn;
		});
	}

	*freeAt {arg index;
		if(bufferArray.notNil, {
			if(bufferArray.isEmpty.not, {
				if(bufferArray[index].notNil, {
					"Free Buffer: ".postin(postWhere, \post, postWin);
					bufferArray[index].postin(postWhere, \ln, postWin).free;
					bufferArray.removeAt(index);
					tags.removeAt(index);
				}, {
					"Index not found".warn;
				});
			}, {
				"Buffers system is empty".warn;
			});
		}, {
			"No buffers found".warn;
		});
	}

	*freeAtAll {arg indexArr;
		var count=0;
		indexArr.do{|index| this.freeAt(index - count); count = count + 1};
	}

	*free {arg tag;
		var resultBuf, bufIndex, symbols;
		if(bufferArray.notNil, {
			symbols = this.tags;
			bufIndex = symbols.indexOfEqual(tag);
			if(bufIndex.notNil, {
				"Free Buffer: ".postin(postWhere, \post, postWin);
				bufferArray[bufIndex].postin(postWhere, \ln, postWin).free;
				bufferArray.removeAt(bufIndex);
				tags.removeAt(bufIndex);
			}, {
				"Tag not found".warn;
			});
		}, {
			"No buffers allocated".warn;
		});
	}

	*freeAll {arg tagArr;
		if(tagArr.notNil, {
			tagArr.do{|tag| this.free(tag);};
		}, {
			bufferArray.do{|item|
				"Free Buffer: ".postin(postWhere, \post, postWin);
				item.postin(postWhere, \ln, postWin).free;
			};
			bufferArray = nil;
			tags = nil;
		});
	}

	*pathToTag {arg path;
		var myPath;
		myPath = PathName.new(path);
		^myPath.fileNameWithoutExtension.asSymbol;
	}

}