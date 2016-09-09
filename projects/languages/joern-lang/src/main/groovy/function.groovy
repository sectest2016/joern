
GraphTraversal.metaClass.functionToLocationStr = {
  delegate.as('func')
  .functionToFile().as('file')
  .select('func', 'file')
  .map { it.get()['file'].values('code').next() + ':' +  it.get()['func'].values('location').next() }  
}

/**
   (Optimized) match-traversals for functions.
*/

GraphTraversal.metaClass.functionToAST = {
	delegate.out(FUNCTION_TO_AST_EDGE)
}


GraphTraversal.metaClass.functionToCFG = {
	delegate.out(FUNCTION_TO_CFG_EDGE)
}


GraphTraversal.metaClass.functionToASTNodes = {
	delegate.functionToAST().astNodes()
}

/**
	functionToStatements, implemented as a CFG traversal. This does not benefit from
 	potential index lookups, but is lazy, Order is preserved in a depth first traversal.
 */
addStep('functionToStatementsTraverse', {
	delegate
		.functionToCFG()
		.emit()
		.repeat(
			__
			.out(CFG_EDGE)
			.simplePath()
		)
		.dedup()
})

/**
	functionToStatements, implemented with a potential lookup. This may benefit from potential
	index lookups, but is no longer lazy, Order is not preserved.
 */
addStep('functionToStatementsLookup', {
	fids = delegate
			.functionToCFG()
			.values('functionId').collect()
	g.V()
			.has('functionId',P.within(fids))
			.has('isCFGNode','True')
})

addStep('functionToStatements', {
	delegate.functionToStatementsTraverse()
}

GraphTraversal.metaClass.functionsToASTNodesOfType = { def args; def type = args[0];
	delegate.transform{ queryNodeIndex('functionId:' + it.id + " AND $NODE_TYPE:$type") }
	 .scatter()
}

GraphTraversal.metaClass.functionToFile = {
	delegate.in(FILE_TO_FUNCTION_EDGE)
}

/**
 * For a function node, get callers using `name` property.
 **/

GraphTraversal.metaClass.functionToCallers = {
	_().transform{

		funcName = it.name
		funcName = funcName.split(' ')[-1].trim()
	   	funcName = funcName.replace('*', '')

		getCallsTo(funcName)
	}.scatter()
}
